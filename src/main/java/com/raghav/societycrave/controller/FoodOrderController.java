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
    public List<FoodOrder> getAllOrders() {
        return foodOrderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public FoodOrder getOrderById(@PathVariable Long id) {
        return foodOrderService.getOrderById(id);
    }

    @GetMapping("/status")
    public List<FoodOrder> getOrdersByStatus(@RequestParam String status) {
        return foodOrderService.getOrdersByStatus(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodOrder createOrder(@RequestBody FoodOrder order, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
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
    public FoodOrder updateOrder(@PathVariable Long id, @Valid @RequestBody FoodOrder orderDetails) {
        return foodOrderService.updateOrder(id, orderDetails);
    }

    @PutMapping("/{id}/status")
    public FoodOrder updateOrderStatus(@PathVariable Long id,
                                       @RequestParam String status,
                                       @RequestParam(value = "acceptedBy", required = false) String acceptedBy) {
        return foodOrderService.updateOrderStatus(id, status, acceptedBy);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        foodOrderService.deleteOrder(id);
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
}
