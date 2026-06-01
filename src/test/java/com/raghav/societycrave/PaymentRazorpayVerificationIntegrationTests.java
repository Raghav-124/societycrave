package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.config.RazorpayProperties;
import com.raghav.societycrave.entity.Payment;
import com.raghav.societycrave.repository.FoodOrderRepository;
import com.raghav.societycrave.repository.PaymentRepository;
import com.raghav.societycrave.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentRazorpayVerificationIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Other Society";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @Autowired
    private FakeRazorpayService razorpayService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        foodOrderRepository.deleteAll();
        razorpayService.reset();
    }

    @Test
    void customerCanVerifyOwnPaymentWithValidSignature() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);
        razorpayService.prepareVerificationResult(true);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.gatewayOrderId").value("order_rzp_123"))
                .andExpect(jsonPath("$.gatewayPaymentId").value("pay_rzp_123"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.keySecret").doesNotExist());

        Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("PAID");
        assertThat(saved.getGatewayPaymentId()).isEqualTo("pay_rzp_123");
        assertThat(saved.getGatewaySignature()).isEqualTo("sig_rzp_123");
        assertThat(saved.getPaymentDate()).isEqualTo(LocalDate.now());
        assertThat(razorpayService.getVerifyCallCount()).isEqualTo(1);
    }

    @Test
    void invalidSignatureDoesNotMarkPaymentPaid() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);
        razorpayService.prepareVerificationResult(false);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_bad", "sig_bad")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Razorpay signature verification failed."));

        Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("DUE");
        assertThat(saved.getGatewayPaymentId()).isNull();
        assertThat(saved.getGatewaySignature()).isNull();
        assertThat(saved.getPaymentDate()).isNull();
    }

    @Test
    void missingGatewayOrderIdReturnsClearError() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", null, null, null, null);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment does not have a Razorpay order to verify."));
    }

    @Test
    void requestRazorpayOrderIdMismatchReturnsClearError() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_saved", null, null, null);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_other", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Razorpay order id does not match the saved payment gateway order."));
    }

    @Test
    void chefCannotVerifyPayment() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherCustomerCannotVerifySameSocietyPayment() throws Exception {
        Payment payment = seedPayment("other.green@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossSocietyVerificationReturnsForbidden() throws Exception {
        Payment payment = seedPayment("other@example.com", OTHER_SOCIETY, "DUE", "order_rzp_123", null, null, null);

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingPaymentReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", 999999L)
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alreadyPaidSameGatewayPaymentIdIsIdempotent() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "PAID", "order_rzp_123", "pay_rzp_123", "sig_saved", LocalDate.of(2026, 6, 1));

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_new")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.gatewayPaymentId").value("pay_rzp_123"))
                .andExpect(jsonPath("$.verified").value(true));

        Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(saved.getGatewaySignature()).isEqualTo("sig_saved");
        assertThat(saved.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(razorpayService.getVerifyCallCount()).isZero();
    }

    @Test
    void alreadyPaidDifferentGatewayPaymentIdReturnsConflict() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "PAID", "order_rzp_123", "pay_saved", "sig_saved", LocalDate.of(2026, 6, 1));

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_other", "sig_new")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment is already verified with a different Razorpay payment id."));
    }

    @Test
    void razorpayDisabledReturnsClearError() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);
        razorpayService.prepareVerificationFailure("Razorpay is not enabled");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Razorpay is not enabled"));
    }

    @Test
    void missingRazorpayKeysReturnsClearError() throws Exception {
        Payment payment = seedPayment("raghav@example.com", GREEN_SOCIETY, "DUE", "order_rzp_123", null, null, null);
        razorpayService.prepareVerificationFailure("Razorpay keys are not configured");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/verify", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVerifyPayload("order_rzp_123", "pay_rzp_123", "sig_rzp_123")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Razorpay keys are not configured"));
    }

    private String validVerifyPayload(String orderId, String paymentId, String signature) {
        return """
                {
                  "razorpayOrderId": "%s",
                  "razorpayPaymentId": "%s",
                  "razorpaySignature": "%s"
                }
                """.formatted(orderId, paymentId, signature);
    }

    private Payment seedPayment(String residentEmail,
                                String societyName,
                                String status,
                                String gatewayOrderId,
                                String gatewayPaymentId,
                                String gatewaySignature,
                                LocalDate paymentDate) {
        Payment payment = new Payment();
        payment.setResidentName("Raghav Agrawal");
        payment.setFlatNumber("A-101");
        payment.setResidentEmail(residentEmail);
        payment.setSocietyName(societyName);
        payment.setAmount(new BigDecimal("499.00"));
        payment.setDueDate(LocalDate.of(2026, 6, 2));
        payment.setPaymentDate(paymentDate);
        payment.setStatus(status);
        payment.setPaymentMethod("UPI");
        payment.setOrderId(101L);
        payment.setGatewayOrderId(gatewayOrderId);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setGatewaySignature(gatewaySignature);
        return paymentRepository.save(payment);
    }

    private String loginCustomerToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "raghav@example.com",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private String registerChefToken() throws Exception {
        String suffix = uniqueSuffix();
        String email = "verify-chef+" + suffix + "@example.com";
        String flatNumber = "V-" + suffix.toUpperCase(Locale.ROOT);

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "Verify Chef",
                                  "email": "%s",
                                  "chefCuisine": "North Indian",
                                  "flatNumber": "%s",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """.formatted(email, flatNumber)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @TestConfiguration
    static class FakeRazorpayConfiguration {

        @Bean
        @Primary
        FakeRazorpayService fakeRazorpayService() {
            RazorpayProperties properties = new RazorpayProperties();
            properties.setEnabled(false);
            properties.setCurrency("INR");
            return new FakeRazorpayService(properties);
        }
    }

    static class FakeRazorpayService extends RazorpayService {

        private GatewayOrderResult nextOrderResult;
        private IllegalStateException nextCreateFailure;
        private Boolean nextVerifyResult;
        private IllegalStateException nextVerifyFailure;
        private String keyId = "rzp_test_fake";
        private String currency = "INR";
        private int createCallCount;
        private int verifyCallCount;

        FakeRazorpayService(RazorpayProperties properties) {
            super(properties);
        }

        void reset() {
            this.nextOrderResult = null;
            this.nextCreateFailure = null;
            this.nextVerifyResult = null;
            this.nextVerifyFailure = null;
            this.keyId = "rzp_test_fake";
            this.currency = "INR";
            this.createCallCount = 0;
            this.verifyCallCount = 0;
        }

        void prepareVerificationResult(boolean result) {
            this.nextVerifyFailure = null;
            this.nextVerifyResult = result;
        }

        void prepareVerificationFailure(String message) {
            this.nextVerifyResult = null;
            this.nextVerifyFailure = new IllegalStateException(message);
        }

        int getVerifyCallCount() {
            return verifyCallCount;
        }

        @Override
        public String getKeyId() {
            return keyId;
        }

        @Override
        public String getCurrency() {
            return currency;
        }

        @Override
        public GatewayOrderResult createGatewayOrder(BigDecimal amount, String receipt, Map<String, String> notes) {
            createCallCount++;
            if (nextCreateFailure != null) {
                throw nextCreateFailure;
            }
            return Objects.requireNonNull(nextOrderResult, "Fake Razorpay order result must be configured");
        }

        @Override
        public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
            verifyCallCount++;
            if (nextVerifyFailure != null) {
                throw nextVerifyFailure;
            }
            return Objects.requireNonNull(nextVerifyResult, "Fake Razorpay verification result must be configured");
        }
    }
}
