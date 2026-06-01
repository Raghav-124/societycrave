package com.raghav.societycrave;

import com.raghav.societycrave.config.RazorpayProperties;
import com.raghav.societycrave.service.RazorpayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "razorpay.enabled=false",
        "razorpay.key-id=",
        "razorpay.key-secret=",
        "razorpay.currency=INR"
})
class RazorpayServiceTests {

    @Autowired
    private RazorpayProperties razorpayProperties;

    @Autowired
    private RazorpayService razorpayService;

    @Test
    void applicationStartsWithRazorpayDisabledAndEmptyKeys() {
        assertThat(razorpayProperties).isNotNull();
        assertThat(razorpayService).isNotNull();
    }

    @Test
    void razorpayPropertiesLoadDefaultValues() {
        assertThat(razorpayProperties.isEnabled()).isFalse();
        assertThat(razorpayProperties.getKeyId()).isBlank();
        assertThat(razorpayProperties.getKeySecret()).isBlank();
        assertThat(razorpayProperties.getCurrency()).isEqualTo("INR");
    }

    @Test
    void razorpayServiceIsDisabledByDefault() {
        assertThat(razorpayService.isEnabled()).isFalse();
    }

    @Test
    void createGatewayOrderFailsClearlyWhenDisabled() {
        assertThatThrownBy(() -> razorpayService.createGatewayOrder(
                new BigDecimal("499.99"),
                "receipt-001",
                Map.of("paymentId", "1")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Razorpay is not enabled");
    }

    @Test
    void createGatewayOrderFailsClearlyWhenEnabledWithoutKeys() {
        RazorpayProperties properties = new RazorpayProperties();
        properties.setEnabled(true);
        properties.setKeyId("");
        properties.setKeySecret("");
        properties.setCurrency("INR");

        RazorpayService enabledWithoutKeys = new RazorpayService(properties);

        assertThatThrownBy(() -> enabledWithoutKeys.createGatewayOrder(
                new BigDecimal("499.99"),
                "receipt-002",
                Map.of("paymentId", "2")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Razorpay keys are not configured");
    }
}
