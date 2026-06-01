package com.raghav.societycrave.controller;

import com.raghav.societycrave.dto.auth.AuthResponse;
import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.security.JwtAuthenticatedUser;
import com.raghav.societycrave.service.AuthService;
import com.raghav.societycrave.service.FoodOrderService;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;
    private final AuthService authService;
    private final Validator validator;

    public FoodOrderController(FoodOrderService foodOrderService,
                               AuthService authService,
                               Validator validator) {
        this.foodOrderService = foodOrderService;
        this.authService = authService;
        this.validator = validator;
    }

    @GetMapping
    public List<FoodOrder> getAllOrders(Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        return foodOrderService.getAllOrdersForSociety(principal.societyName());
    }

    @GetMapping("/{id}")
    public FoodOrder getOrderById(@PathVariable Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        return foodOrderService.getOrderByIdForSociety(id, principal.societyName());
    }

    @GetMapping("/status")
    public List<FoodOrder> getOrdersByStatus(@RequestParam String status, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        return foodOrderService.getOrdersByStatusForSociety(status, principal.societyName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodOrder createOrder(@RequestBody FoodOrder order, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can create orders.");
        validateRequestedSociety(order.getSocietyName(), principal.societyName());

        AuthResponse profile = authService.getCurrentProfile(
                principal.subject(),
                principal.role(),
                principal.societyName()
        );

        order.setCustomerName(profile.displayName());
        order.setFlatNumber(profile.flatNumber());
        order.setSocietyName(profile.societyName());
        validateOrder(order);
        return foodOrderService.saveOrder(order);
    }

    @PutMapping("/{id}")
    public FoodOrder updateOrder(@PathVariable Long id,
                                 @Valid @RequestBody FoodOrder orderDetails,
                                 Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can edit orders.");
        return foodOrderService.updateOrderForSociety(id, orderDetails, principal.societyName());
    }

    @PutMapping("/{id}/status")
    public FoodOrder updateOrderStatus(@PathVariable Long id,
                                       @RequestParam String status,
                                       @RequestParam(value = "acceptedBy", required = false) String acceptedBy,
                                       Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireStatusRole(principal, status);
        return foodOrderService.updateOrderStatusForSociety(id, status, acceptedBy, principal.societyName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can delete orders.");
        foodOrderService.deleteOrderForSociety(id, principal.societyName());
    }

    private JwtAuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT.");
        }
        return principal;
    }

    private void validateRequestedSociety(String requestedSociety, String authenticatedSociety) {
        String requested = normalize(requestedSociety);
        String authenticated = normalize(authenticatedSociety);
        if (authenticated == null || authenticated.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is missing society scope.");
        }
        if (requested != null && !requested.equalsIgnoreCase(authenticated)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-society order creation is not allowed.");
        }
    }

    private void validateOrder(FoodOrder order) {
        Set<ConstraintViolation<FoodOrder>> violations = validator.validate(order);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    violations.iterator().next().getMessage()
            );
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void requireCustomer(JwtAuthenticatedUser principal, String message) {
        if (!principal.isCustomer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void requireStatusRole(JwtAuthenticatedUser principal, String status) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus == null || normalizedStatus.isBlank()) {
            return;
        }

        String targetStatus = normalizedStatus.toUpperCase();
        if ((targetStatus.equals("ACCEPTED") || targetStatus.equals("DELIVERED")) && !principal.isChef()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only chefs can accept or deliver orders.");
        }
        if (targetStatus.equals("CANCELLED") && !principal.isCustomer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can cancel orders.");
        }
    }
}
