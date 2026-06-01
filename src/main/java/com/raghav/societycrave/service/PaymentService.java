package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.entity.Payment;
import com.raghav.societycrave.repository.FoodOrderRepository;
import com.raghav.societycrave.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PaymentService {

    private static final String DEFAULT_PAYMENT_STATUS = "DUE";

    private final PaymentRepository paymentRepository;
    private final FoodOrderRepository foodOrderRepository;

    public PaymentService(PaymentRepository paymentRepository, FoodOrderRepository foodOrderRepository) {
        this.paymentRepository = paymentRepository;
        this.foodOrderRepository = foodOrderRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getAllPaymentsForSociety(String societyName) {
        return paymentRepository.findBySocietyNameIgnoreCase(requireSocietyScope(societyName));
    }

    public List<Payment> getAllPaymentsForCustomer(String societyName, String residentEmail) {
        return paymentRepository.findBySocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
                requireSocietyScope(societyName),
                requireResidentEmail(residentEmail)
        );
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found with id " + id));
    }

    public Payment getPaymentByIdForSociety(Long id, String societyName) {
        Payment payment = getPaymentById(id);
        if (!requireSocietyScope(societyName).equalsIgnoreCase(normalize(payment.getSocietyName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society payment access is not allowed.");
        }
        return payment;
    }

    public Payment getPaymentByIdForCustomer(Long id, String societyName, String residentEmail) {
        Payment payment = getPaymentByIdForSociety(id, societyName);
        enforceCustomerReadAccess(payment, residentEmail);
        return payment;
    }

    public Payment savePayment(Payment payment) {
        normalizePayment(payment);
        return paymentRepository.save(payment);
    }

    public Payment createPaymentForCustomer(Payment paymentRequest, String societyName, String residentEmail) {
        validateRequestedSociety(paymentRequest.getSocietyName(), societyName);

        if (paymentRequest.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required to create payment");
        }

        FoodOrder order = getOrderForCustomerPaymentCreation(
                paymentRequest.getOrderId(),
                requireSocietyScope(societyName),
                requireResidentEmail(residentEmail)
        );

        Payment payment = new Payment();
        payment.setResidentName(order.getCustomerName());
        payment.setFlatNumber(order.getFlatNumber());
        payment.setResidentEmail(requireResidentEmail(residentEmail));
        payment.setSocietyName(requireSocietyScope(societyName));
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotalAmount());
        payment.setDueDate(paymentRequest.getDueDate());
        payment.setPaymentDate(null);
        payment.setStatus(DEFAULT_PAYMENT_STATUS);
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        normalizePayment(payment);
        return paymentRepository.save(payment);
    }

    public Payment updatePayment(Long id, Payment paymentDetails) {
        Payment payment = getPaymentById(id);
        payment.setResidentName(paymentDetails.getResidentName());
        payment.setFlatNumber(paymentDetails.getFlatNumber());
        payment.setAmount(paymentDetails.getAmount());
        payment.setDueDate(paymentDetails.getDueDate());
        payment.setPaymentDate(paymentDetails.getPaymentDate());
        payment.setStatus(paymentDetails.getStatus());
        payment.setPaymentMethod(paymentDetails.getPaymentMethod());
        payment.setResidentEmail(paymentDetails.getResidentEmail());
        payment.setSocietyName(paymentDetails.getSocietyName());
        payment.setOrderId(paymentDetails.getOrderId());
        normalizePayment(payment);
        return paymentRepository.save(payment);
    }

    public Payment updatePaymentForCustomer(Long id, Payment paymentDetails, String societyName, String residentEmail) {
        Payment payment = getPaymentByIdForSociety(id, societyName);
        enforceCustomerOwnership(payment, residentEmail);

        normalizePayment(paymentDetails);

        String trustedResidentName = payment.getResidentName();
        String trustedFlatNumber = payment.getFlatNumber();
        String trustedResidentEmail = payment.getResidentEmail();
        String trustedSocietyName = payment.getSocietyName();
        Long trustedOrderId = payment.getOrderId();
        String trustedStatus = payment.getStatus();
        java.time.LocalDate trustedPaymentDate = payment.getPaymentDate();
        java.math.BigDecimal trustedAmount = payment.getAmount();

        payment.setAmount(trustedAmount);
        payment.setDueDate(paymentDetails.getDueDate());
        payment.setPaymentDate(trustedPaymentDate);
        payment.setStatus(trustedStatus);
        payment.setPaymentMethod(paymentDetails.getPaymentMethod());
        payment.setResidentName(trustedResidentName);
        payment.setFlatNumber(trustedFlatNumber);
        payment.setResidentEmail(trustedResidentEmail);
        payment.setSocietyName(trustedSocietyName);
        payment.setOrderId(trustedOrderId);
        normalizePayment(payment);
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found with id " + id);
        }
        paymentRepository.deleteById(id);
    }

    public void deletePaymentForCustomer(Long id, String societyName, String residentEmail) {
        Payment payment = getPaymentByIdForSociety(id, societyName);
        enforceCustomerOwnership(payment, residentEmail);
        paymentRepository.delete(payment);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatusIgnoreCase(status);
    }

    public List<Payment> getPaymentsByStatusForSociety(String status, String societyName) {
        return paymentRepository.findByStatusIgnoreCaseAndSocietyNameIgnoreCase(
                status,
                requireSocietyScope(societyName)
        );
    }

    public List<Payment> getPaymentsByStatusForCustomer(String status, String societyName, String residentEmail) {
        return paymentRepository.findByStatusIgnoreCaseAndSocietyNameIgnoreCaseAndResidentEmailIgnoreCase(
                status,
                requireSocietyScope(societyName),
                requireResidentEmail(residentEmail)
        );
    }

    public void validateRequestedSociety(String requestedSociety, String authenticatedSociety) {
        String requested = normalize(requestedSociety);
        String authenticated = normalize(authenticatedSociety);
        if (authenticated.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        if (!requested.isBlank() && !requested.equalsIgnoreCase(authenticated)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society payment creation is not allowed.");
        }
    }

    private void normalizePayment(Payment payment) {
        if (payment.getResidentName() != null) {
            payment.setResidentName(payment.getResidentName().trim());
        }
        if (payment.getFlatNumber() != null) {
            payment.setFlatNumber(payment.getFlatNumber().trim());
        }
        if (payment.getResidentEmail() != null) {
            payment.setResidentEmail(payment.getResidentEmail().trim());
        }
        if (payment.getSocietyName() != null) {
            payment.setSocietyName(payment.getSocietyName().trim());
        }
        if (payment.getStatus() != null) {
            payment.setStatus(payment.getStatus().trim().toUpperCase());
        }
        if (payment.getPaymentMethod() != null) {
            payment.setPaymentMethod(payment.getPaymentMethod().trim());
        }
    }

    private FoodOrder getOrderForCustomerPaymentCreation(Long orderId, String societyName, String residentEmail) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + orderId));

        if (!societyName.equalsIgnoreCase(normalize(order.getSocietyName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society payment creation is not allowed.");
        }

        String orderCustomerEmail = normalize(order.getCustomerEmail());
        if (orderCustomerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payments require orders with verified customer ownership.");
        }

        if (!orderCustomerEmail.equalsIgnoreCase(residentEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only create payments for their own orders.");
        }

        return order;
    }

    private void enforceCustomerOwnership(Payment payment, String residentEmail) {
        String paymentEmail = normalize(payment.getResidentEmail());
        String principalEmail = requireResidentEmail(residentEmail);
        if (paymentEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only manage payments with verified ownership.");
        }
        if (!paymentEmail.equalsIgnoreCase(principalEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only manage their own payments.");
        }
    }

    private void enforceCustomerReadAccess(Payment payment, String residentEmail) {
        String paymentEmail = normalize(payment.getResidentEmail());
        String principalEmail = requireResidentEmail(residentEmail);
        if (paymentEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only view payments with verified ownership.");
        }
        if (!paymentEmail.equalsIgnoreCase(principalEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only view their own payments.");
        }
    }

    private String requireSocietyScope(String societyName) {
        String normalized = normalize(societyName);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        return normalized;
    }

    private String requireResidentEmail(String residentEmail) {
        String normalized = normalize(residentEmail);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Resident session is missing email identity.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
