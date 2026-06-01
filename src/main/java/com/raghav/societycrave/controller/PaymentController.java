package com.raghav.societycrave.controller;

import com.raghav.societycrave.dto.payment.VerifyRazorpayPaymentRequest;
import com.raghav.societycrave.dto.payment.VerifyRazorpayPaymentResponse;
import com.raghav.societycrave.entity.Payment;
import com.raghav.societycrave.dto.payment.CreateRazorpayOrderResponse;
import com.raghav.societycrave.security.JwtAuthenticatedUser;
import com.raghav.societycrave.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getAllPayments(Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return paymentService.getAllPaymentsForCustomer(principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return paymentService.getAllPaymentsForSociety(principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for payment access.");
    }

    @GetMapping("/status")
    public List<Payment> getPaymentsByStatus(@RequestParam String status, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return paymentService.getPaymentsByStatusForCustomer(status, principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return paymentService.getPaymentsByStatusForSociety(status, principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for payment access.");
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (principal.isCustomer()) {
            return paymentService.getPaymentByIdForCustomer(id, principal.societyName(), requireResidentEmail(principal));
        }
        if (principal.isChef()) {
            return paymentService.getPaymentByIdForSociety(id, principal.societyName());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role for payment access.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@RequestBody Payment payment, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can create payments.");
        return paymentService.createPaymentForCustomer(
                payment,
                principal.societyName(),
                requireResidentEmail(principal)
        );
    }

    @PostMapping("/{paymentId}/razorpay/order")
    public CreateRazorpayOrderResponse createRazorpayOrder(@PathVariable Long paymentId, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can create Razorpay orders.");
        return paymentService.createGatewayOrderForCustomer(
                paymentId,
                principal.societyName(),
                requireResidentEmail(principal)
        );
    }

    @PostMapping("/{paymentId}/razorpay/verify")
    public VerifyRazorpayPaymentResponse verifyRazorpayPayment(@PathVariable Long paymentId,
                                                               @Valid @RequestBody VerifyRazorpayPaymentRequest request,
                                                               Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can verify Razorpay payments.");
        return paymentService.verifyGatewayPaymentForCustomer(
                paymentId,
                request,
                principal.societyName(),
                requireResidentEmail(principal)
        );
    }

    @PutMapping("/{id}")
    public Payment updatePayment(@PathVariable Long id, @RequestBody Payment payment, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can update payments.");
        return paymentService.updatePaymentForCustomer(id, payment, principal.societyName(), requireResidentEmail(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long id, Authentication authentication) {
        JwtAuthenticatedUser principal = requireAuthenticatedUser(authentication);
        requireCustomer(principal, "Only customers can delete payments.");
        paymentService.deletePaymentForCustomer(id, principal.societyName(), requireResidentEmail(principal));
    }

    private JwtAuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT.");
        }
        return principal;
    }

    private String requireResidentEmail(JwtAuthenticatedUser principal) {
        String email = normalize(principal.email());
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Resident session is missing email identity.");
        }
        return email;
    }

    private void requireCustomer(JwtAuthenticatedUser principal, String message) {
        if (!principal.isCustomer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
