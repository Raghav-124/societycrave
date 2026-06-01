package com.raghav.societycrave.dto.payment;

public record VerifyRazorpayPaymentResponse(
        Long paymentId,
        String status,
        String gatewayOrderId,
        String gatewayPaymentId,
        boolean verified
) {
}
