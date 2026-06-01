package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.repository.ChefRepository;
import com.raghav.societycrave.repository.FoodOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final ChefRepository chefRepository;

    public FoodOrderService(FoodOrderRepository foodOrderRepository, ChefRepository chefRepository) {
        this.foodOrderRepository = foodOrderRepository;
        this.chefRepository = chefRepository;
    }

    public List<FoodOrder> getAllOrders() {
        return foodOrderRepository.findAll();
    }

    public List<FoodOrder> getAllOrdersForSociety(String societyName) {
        return foodOrderRepository.findBySocietyNameIgnoreCase(requireSocietyScope(societyName));
    }

    public FoodOrder getOrderById(Long id) {
        return foodOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + id));
    }

    public FoodOrder getOrderByIdForSociety(Long id, String societyName) {
        String normalizedSociety = requireSocietyScope(societyName);
        FoodOrder order = getOrderById(id);
        if (!normalizedSociety.equalsIgnoreCase(normalize(order.getSocietyName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society order access is not allowed.");
        }
        return order;
    }

    public FoodOrder saveOrder(FoodOrder order) {
        if (order.getOrderTime() == null) order.setOrderTime(LocalDateTime.now());
        if (order.getStatus() == null) order.setStatus("PLACED");
        if (order.getDiscount() == null) order.setDiscount(java.math.BigDecimal.ZERO);
        if (order.getDeliveryCharge() == null) order.setDeliveryCharge(java.math.BigDecimal.ZERO);
        normalizeOrder(order);
        validateSocietyChefMapping(order);
        return foodOrderRepository.save(order);
    }

    public FoodOrder updateOrder(Long id, FoodOrder details) {
        FoodOrder order = getOrderById(id);
        if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivered orders cannot be updated");
        }
        copyOrderDetails(order, details);
        validateSocietyChefMapping(order);
        return foodOrderRepository.save(order);
    }

    public FoodOrder updateOrderForSociety(Long id, FoodOrder details, String societyName) {
        FoodOrder order = getOrderByIdForSociety(id, societyName);
        String trustedCustomerName = order.getCustomerName();
        String trustedFlatNumber = order.getFlatNumber();
        String trustedSocietyName = order.getSocietyName();

        if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivered orders cannot be updated");
        }

        copyOrderDetails(order, details);
        order.setCustomerName(trustedCustomerName);
        order.setFlatNumber(trustedFlatNumber);
        order.setSocietyName(trustedSocietyName);
        validateSocietyChefMapping(order);
        return foodOrderRepository.save(order);
    }

    public FoodOrder updateOrderStatus(Long id, String status, String acceptedBy) {
        FoodOrder order = getOrderById(id);
        status = status.toUpperCase();
        if (!isValidStatusTransition(order.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + order.getStatus() + " to " + status);
        }
        order.setStatus(status);
        if ("ACCEPTED".equals(status) && acceptedBy != null && !acceptedBy.isBlank()) {
            String acceptedByTrimmed = acceptedBy.trim();
            if (!isChefInSameSociety(acceptedByTrimmed, order.getSocietyName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chef must belong to the same society as the customer order."
                );
            }
            order.setAcceptedBy(acceptedByTrimmed);
        }
        if ("DELIVERED".equals(status)) {
            order.setDeliveryTime(LocalDateTime.now());
        }
        return foodOrderRepository.save(order);
    }

    public FoodOrder updateOrderStatusForSociety(Long id, String status, String acceptedBy, String societyName) {
        FoodOrder order = getOrderByIdForSociety(id, societyName);
        status = status.toUpperCase();
        if (!isValidStatusTransition(order.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + order.getStatus() + " to " + status);
        }
        order.setStatus(status);
        if ("ACCEPTED".equals(status) && acceptedBy != null && !acceptedBy.isBlank()) {
            String acceptedByTrimmed = acceptedBy.trim();
            if (!isChefInSameSociety(acceptedByTrimmed, order.getSocietyName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chef must belong to the same society as the customer order."
                );
            }
            order.setAcceptedBy(acceptedByTrimmed);
        }
        if ("DELIVERED".equals(status)) {
            order.setDeliveryTime(LocalDateTime.now());
        }
        return foodOrderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        if (!foodOrderRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + id);
        }
        foodOrderRepository.deleteById(id);
    }

    public void deleteOrderForSociety(Long id, String societyName) {
        FoodOrder order = getOrderByIdForSociety(id, societyName);
        foodOrderRepository.delete(order);
    }

    public List<FoodOrder> getOrdersByStatus(String status) {
        return foodOrderRepository.findByStatusIgnoreCase(status);
    }

    public List<FoodOrder> getOrdersByStatusForSociety(String status, String societyName) {
        return foodOrderRepository.findByStatusIgnoreCaseAndSocietyNameIgnoreCase(
                status,
                requireSocietyScope(societyName)
        );
    }

    private void normalizeOrder(FoodOrder order) {
        if (order.getCustomerName() != null) order.setCustomerName(order.getCustomerName().trim());
        if (order.getFlatNumber() != null) order.setFlatNumber(order.getFlatNumber().trim());
        if (order.getItems() != null) order.setItems(order.getItems().trim());
        if (order.getAcceptedBy() != null) order.setAcceptedBy(order.getAcceptedBy().trim());
        if (order.getPaymentMethod() != null) order.setPaymentMethod(order.getPaymentMethod().trim());
        if (order.getSocietyName() != null) order.setSocietyName(order.getSocietyName().trim());
    }

    private void copyOrderDetails(FoodOrder target, FoodOrder source) {
        if (source.getCustomerName() != null) target.setCustomerName(source.getCustomerName().trim());
        if (source.getFlatNumber() != null) target.setFlatNumber(source.getFlatNumber().trim());
        if (source.getItems() != null) target.setItems(source.getItems().trim());
        if (source.getTotalAmount() != null) target.setTotalAmount(source.getTotalAmount());
        if (source.getStatus() != null) target.setStatus(source.getStatus().toUpperCase());
        if (source.getAcceptedBy() != null) target.setAcceptedBy(source.getAcceptedBy().trim());
        if (source.getOrderTime() != null) target.setOrderTime(source.getOrderTime());
        if (source.getDeliveryTime() != null) target.setDeliveryTime(source.getDeliveryTime());
        if (source.getDiscount() != null) target.setDiscount(source.getDiscount());
        if (source.getDeliveryCharge() != null) target.setDeliveryCharge(source.getDeliveryCharge());
        if (source.getPaymentMethod() != null) target.setPaymentMethod(source.getPaymentMethod().trim());
        if (source.getSocietyName() != null) target.setSocietyName(source.getSocietyName().trim());
    }

    private boolean isValidStatusTransition(String current, String next) {
        current = current.toUpperCase();
        next = next.toUpperCase();
        return switch (current) {
            case "PLACED" -> next.equals("ACCEPTED") || next.equals("CANCELLED");
            case "ACCEPTED" -> next.equals("DELIVERED") || next.equals("CANCELLED");
            case "DELIVERED", "CANCELLED" -> false;
            default -> false;
        };
    }

    private void validateSocietyChefMapping(FoodOrder order) {
        if (order.getSocietyName() == null || order.getSocietyName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Society name is required for orders.");
        }
        if (order.getAcceptedBy() != null && !order.getAcceptedBy().isBlank()) {
            if (!isChefInSameSociety(order.getAcceptedBy(), order.getSocietyName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Accepted chef must belong to the same society as the customer."
                );
            }
        }
    }

    private boolean isChefInSameSociety(String chefName, String societyName) {
        String normalizedChefName = normalize(chefName);
        String normalizedSociety = normalize(societyName);
        return chefRepository.findAll()
                .stream()
                .anyMatch(chef -> chef.getChefName() != null
                        && chef.getSocietyName() != null
                        && normalize(chef.getChefName()).equalsIgnoreCase(normalizedChefName)
                        && normalize(chef.getSocietyName()).equalsIgnoreCase(normalizedSociety));
    }

    private String requireSocietyScope(String societyName) {
        String normalizedSociety = normalize(societyName);
        if (normalizedSociety.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        return normalizedSociety;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
