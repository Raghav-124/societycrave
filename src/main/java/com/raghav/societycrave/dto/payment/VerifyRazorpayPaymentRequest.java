package com.raghav.societycrave.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record VerifyRazorpayPaymentRequest(
        @NotBlank(message = "Razorpay order id is mandatory") String razorpayOrderId,
        @NotBlank(message = "Razorpay payment id is mandatory") String razorpayPaymentId,
        @NotBlank(message = "Razorpay signature is mandatory") String razorpaySignature
) {
}
