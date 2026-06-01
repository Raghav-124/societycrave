package com.raghav.societycrave.dto.payment;

public record CreateRazorpayOrderResponse(
        Long paymentId,
        String razorpayOrderId,
        String keyId,
        long amount,
        String currency,
        String status
) {
}
