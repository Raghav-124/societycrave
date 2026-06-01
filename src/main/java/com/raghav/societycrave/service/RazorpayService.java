package com.raghav.societycrave.service;

import com.raghav.societycrave.config.RazorpayProperties;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

@Service
public class RazorpayService {

    private final RazorpayProperties razorpayProperties;

    public RazorpayService(RazorpayProperties razorpayProperties) {
        this.razorpayProperties = razorpayProperties;
    }

    public boolean isEnabled() {
        return razorpayProperties.isEnabled();
    }

    public Order createGatewayOrder(BigDecimal amount, String receipt, Map<String, String> notes) {
        requireEnabledAndConfigured();

        Objects.requireNonNull(amount, "amount must not be null");

        try {
            RazorpayClient client = new RazorpayClient(
                    razorpayProperties.getKeyId().trim(),
                    razorpayProperties.getKeySecret().trim()
            );

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", convertAmountToPaise(amount));
            orderRequest.put("currency", normalizedCurrency());
            orderRequest.put("receipt", normalize(receipt));
            if (notes != null && !notes.isEmpty()) {
                orderRequest.put("notes", new JSONObject(notes));
            }

            return client.orders.create(orderRequest);
        } catch (RazorpayException exception) {
            throw new IllegalStateException("Unable to create Razorpay order", exception);
        }
    }

    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        requireEnabledAndConfigured();

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", normalize(razorpayOrderId));
            options.put("razorpay_payment_id", normalize(razorpayPaymentId));
            options.put("razorpay_signature", normalize(razorpaySignature));
            return Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret().trim());
        } catch (RazorpayException exception) {
            throw new IllegalStateException("Unable to verify Razorpay signature", exception);
        }
    }

    private void requireEnabledAndConfigured() {
        if (!razorpayProperties.isEnabled()) {
            throw new IllegalStateException("Razorpay is not enabled");
        }

        if (normalize(razorpayProperties.getKeyId()).isBlank()
                || normalize(razorpayProperties.getKeySecret()).isBlank()) {
            throw new IllegalStateException("Razorpay keys are not configured");
        }
    }

    private String normalizedCurrency() {
        String currency = normalize(razorpayProperties.getCurrency());
        return currency.isBlank() ? "INR" : currency.toUpperCase();
    }

    private int convertAmountToPaise(BigDecimal amount) {
        return amount
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .intValueExact();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
