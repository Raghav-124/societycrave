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
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
class PaymentRazorpayOrderIntegrationTests {

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
    void customerCanCreateRazorpayOrderForOwnDuePayment() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("499.00"),
                "DUE",
                "UPI",
                101L,
                null,
                null
        );

        razorpayService.prepareSuccess("order_rzp_123", 49900L, "INR", "rzp_test_123");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.razorpayOrderId").value("order_rzp_123"))
                .andExpect(jsonPath("$.keyId").value("rzp_test_123"))
                .andExpect(jsonPath("$.amount").value(49900))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("DUE"))
                .andExpect(jsonPath("$.keySecret").doesNotExist());

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getGatewayOrderId()).isEqualTo("order_rzp_123");
        assertThat(savedPayment.getGatewayPaymentId()).isNull();
        assertThat(savedPayment.getGatewaySignature()).isNull();
        assertThat(savedPayment.getStatus()).isEqualTo("DUE");
        assertThat(razorpayService.getCreateCallCount()).isEqualTo(1);
    }

    @Test
    void repeatedCreateRazorpayOrderCallIsIdempotent() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("615.50"),
                "DUE",
                "UPI",
                201L,
                null,
                null
        );

        razorpayService.prepareSuccess("order_rzp_once", 61550L, "INR", "rzp_test_123");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_rzp_once"));

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_rzp_once"))
                .andExpect(jsonPath("$.amount").value(61550))
                .andExpect(jsonPath("$.currency").value("INR"));

        assertThat(razorpayService.getCreateCallCount()).isEqualTo(1);
    }

    @Test
    void customerCannotCreateRazorpayOrderForAnotherCustomersPayment() throws Exception {
        Payment payment = seedPayment(
                "Other Resident",
                "B-202",
                "other.green@example.com",
                GREEN_SOCIETY,
                new BigDecimal("275.00"),
                "DUE",
                "Cash",
                301L,
                null,
                null
        );

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());

        assertThat(razorpayService.getCreateCallCount()).isZero();
    }

    @Test
    void customerCannotCreateRazorpayOrderForCrossSocietyPayment() throws Exception {
        Payment payment = seedPayment(
                "Other Society Resident",
                "C-303",
                "other@example.com",
                OTHER_SOCIETY,
                new BigDecimal("315.00"),
                "DUE",
                "Cash",
                302L,
                null,
                null
        );

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());

        assertThat(razorpayService.getCreateCallCount()).isZero();
    }

    @Test
    void chefCannotCreateRazorpayOrder() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("275.00"),
                "DUE",
                "UPI",
                401L,
                null,
                null
        );

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isForbidden());

        assertThat(razorpayService.getCreateCallCount()).isZero();
    }

    @Test
    void missingPaymentReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", 999999L)
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isNotFound());

        assertThat(razorpayService.getCreateCallCount()).isZero();
    }

    @Test
    void razorpayDisabledReturnsClearError() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("525.00"),
                "DUE",
                "UPI",
                501L,
                null,
                null
        );

        razorpayService.prepareFailure("Razorpay is not enabled");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Razorpay is not enabled"));
    }

    @Test
    void missingRazorpayKeysWhileEnabledReturnsClearError() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("525.00"),
                "DUE",
                "UPI",
                601L,
                null,
                null
        );

        razorpayService.prepareFailure("Razorpay keys are not configured");

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Razorpay keys are not configured"));
    }

    @Test
    void paidPaymentCannotCreateRazorpayOrder() throws Exception {
        Payment payment = seedPayment(
                "Raghav Agrawal",
                "A-101",
                "raghav@example.com",
                GREEN_SOCIETY,
                new BigDecimal("525.00"),
                "PAID",
                "UPI",
                701L,
                LocalDate.of(2026, 6, 1),
                null
        );

        mockMvc.perform(post("/api/payments/{paymentId}/razorpay/order", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Razorpay order can only be created for DUE or PENDING payments."));

        assertThat(razorpayService.getCreateCallCount()).isZero();
    }

    private Payment seedPayment(String residentName,
                                String flatNumber,
                                String residentEmail,
                                String societyName,
                                BigDecimal amount,
                                String status,
                                String paymentMethod,
                                Long orderId,
                                LocalDate paymentDate,
                                String gatewayOrderId) {
        Payment payment = new Payment();
        payment.setResidentName(residentName);
        payment.setFlatNumber(flatNumber);
        payment.setResidentEmail(residentEmail);
        payment.setSocietyName(societyName);
        payment.setAmount(amount);
        payment.setDueDate(LocalDate.of(2026, 6, 2));
        payment.setPaymentDate(paymentDate);
        payment.setStatus(status);
        payment.setPaymentMethod(paymentMethod);
        payment.setOrderId(orderId);
        payment.setGatewayOrderId(gatewayOrderId);
        payment.setGatewayPaymentId(null);
        payment.setGatewaySignature(null);
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
        String email = "gateway-chef+" + suffix + "@example.com";
        String flatNumber = "G-" + suffix.toUpperCase(Locale.ROOT);

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "Gateway Chef",
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
        private IllegalStateException nextFailure;
        private String keyId = "rzp_test_fake";
        private String currency = "INR";
        private int createCallCount;

        FakeRazorpayService(RazorpayProperties properties) {
            super(properties);
        }

        void prepareSuccess(String gatewayOrderId, long amountInPaise, String currency, String keyId) {
            this.nextFailure = null;
            this.nextOrderResult = new GatewayOrderResult(gatewayOrderId, amountInPaise, currency);
            this.currency = currency;
            this.keyId = keyId;
        }

        void prepareFailure(String message) {
            this.nextOrderResult = null;
            this.nextFailure = new IllegalStateException(message);
        }

        int getCreateCallCount() {
            return createCallCount;
        }

        void reset() {
            this.nextOrderResult = null;
            this.nextFailure = null;
            this.keyId = "rzp_test_fake";
            this.currency = "INR";
            this.createCallCount = 0;
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
            if (nextFailure != null) {
                throw nextFailure;
            }
            return Objects.requireNonNull(nextOrderResult, "Fake Razorpay order result must be configured");
        }
    }
}
