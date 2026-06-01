package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.entity.Payment;
import com.raghav.societycrave.repository.FoodOrderRepository;
import com.raghav.societycrave.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentSecurityIntegrationTests {

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

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        foodOrderRepository.deleteAll();
    }

    @Test
    void paymentCreationRequiresOrderId() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("orderId is required to create payment"));
    }

    @Test
    void paymentCreationRejectsNonExistingOrderId() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 999999,
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentCreationRejectsAnotherCustomersSameSocietyOrder() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "other.green@example.com", "B-202", GREEN_SOCIETY, new BigDecimal("410.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentCreationRejectsCrossSocietyOrder() throws Exception {
        FoodOrder order = seedOrder("Other Society Resident", "other@example.com", "C-303", OTHER_SOCIETY, new BigDecimal("510.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentCreationRejectsLegacyOrderWithNullCustomerEmail() throws Exception {
        FoodOrder order = seedOrder("Legacy Resident", null, "L-101", GREEN_SOCIETY, new BigDecimal("275.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentCreationDerivesAmountAndIdentityFromOrderAndJwt() throws Exception {
        FoodOrder order = seedOrder("Order Owner", "raghav@example.com", "A-101", GREEN_SOCIETY, new BigDecimal("499.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "residentName": "Fake Resident",
                                  "flatNumber": "X-999",
                                  "residentEmail": "fake@example.com",
                                  "societyName": "%s",
                                  "amount": 999.00,
                                  "status": "PAID",
                                  "paymentDate": "2026-06-01",
                                  "dueDate": "2026-06-03",
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId(), GREEN_SOCIETY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.residentName").value("Order Owner"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.amount").value(499.0))
                .andExpect(jsonPath("$.status").value("DUE"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-03"))
                .andExpect(jsonPath("$.paymentDate").isEmpty());
    }

    @Test
    void paymentCreationRejectsMismatchedBodySociety() throws Exception {
        FoodOrder order = seedOrder("Order Owner", "raghav@example.com", "A-101", GREEN_SOCIETY, new BigDecimal("320.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "societyName": "%s",
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId(), OTHER_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefCannotCreatePayment() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, new BigDecimal("215.00"), "PLACED");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + registerChefToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerGetsOnlyOwnPayments() throws Exception {
        seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);
        seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "PAID", "Cash", 102L, LocalDate.of(2026, 6, 1));
        seedPayment("Other Society", "C-303", "other@example.com", OTHER_SOCIETY, new BigDecimal("450.00"), "DUE", "Card", 103L, null);

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY));
    }

    @Test
    void customerGetsOnlyOwnMatchingStatusPayments() throws Exception {
        seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);
        seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("300.00"), "PAID", "Cash", 102L, LocalDate.of(2026, 6, 1));
        seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "DUE", "Card", 103L, null);

        mockMvc.perform(get("/api/payments/status")
                        .param("status", "DUE")
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$[0].status").value("DUE"));
    }

    @Test
    void customerGetsOwnPaymentById() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"));
    }

    @Test
    void customerCannotGetAnotherResidentsSameSocietyPayment() throws Exception {
        Payment payment = seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "DUE", "Cash", 102L, null);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotGetLegacyNullResidentEmailPayment() throws Exception {
        Payment payment = seedPayment("Legacy Resident", "L-101", null, GREEN_SOCIETY, new BigDecimal("175.00"), "DUE", "Cash", 104L, null);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefGetsSameSocietyPayments() throws Exception {
        seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);
        seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "PAID", "Cash", 102L, LocalDate.of(2026, 6, 1));
        seedPayment("Other Society", "C-303", "other@example.com", OTHER_SOCIETY, new BigDecimal("450.00"), "DUE", "Card", 103L, null);

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void chefGetsSameSocietyMatchingStatusPayments() throws Exception {
        seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);
        seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "DUE", "Cash", 102L, null);
        seedPayment("Other Society", "C-303", "other@example.com", OTHER_SOCIETY, new BigDecimal("450.00"), "DUE", "Card", 103L, null);
        seedPayment("Green Paid", "D-404", "green.paid@example.com", GREEN_SOCIETY, new BigDecimal("500.00"), "PAID", "Card", 104L, LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/payments/status")
                        .param("status", "DUE")
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void chefGetsSameSocietyPaymentById() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()));
    }

    @Test
    void chefCrossSocietyPaymentReadReturnsForbidden() throws Exception {
        Payment payment = seedPayment("Other Society", "C-303", "other@example.com", OTHER_SOCIETY, new BigDecimal("450.00"), "DUE", "Card", 103L, null);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanUpdateOnlySafeOwnPaymentFields() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(put("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "residentName": "Fake Resident",
                                  "flatNumber": "X-999",
                                  "residentEmail": "fake@example.com",
                                  "societyName": "%s",
                                  "orderId": 999,
                                  "amount": 299.00,
                                  "dueDate": "2026-06-03",
                                  "paymentDate": "2026-06-01",
                                  "status": "PAID",
                                  "paymentMethod": "Card"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.status").value("DUE"))
                .andExpect(jsonPath("$.paymentMethod").value("Card"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-03"))
                .andExpect(jsonPath("$.paymentDate").isEmpty());
    }

    @Test
    void customerCannotUpdateAnotherResidentsPayment() throws Exception {
        Payment payment = seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "DUE", "Cash", 102L, null);

        mockMvc.perform(put("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUpdateLegacyNullResidentEmailPayment() throws Exception {
        Payment payment = seedPayment("Legacy Resident", "L-101", null, GREEN_SOCIETY, new BigDecimal("175.00"), "DUE", "Cash", 104L, null);

        mockMvc.perform(put("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefUpdateReturnsForbidden() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(put("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "Card"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanDeleteOwnPayment() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(delete("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isNoContent());

        assertThat(paymentRepository.existsById(payment.getId())).isFalse();
    }

    @Test
    void customerCannotDeleteAnotherResidentsPayment() throws Exception {
        Payment payment = seedPayment("Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, new BigDecimal("350.00"), "DUE", "Cash", 102L, null);

        mockMvc.perform(delete("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());

        assertThat(paymentRepository.existsById(payment.getId())).isTrue();
    }

    @Test
    void chefDeleteReturnsForbidden() throws Exception {
        Payment payment = seedPayment("Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, new BigDecimal("250.00"), "DUE", "UPI", 101L, null);

        mockMvc.perform(delete("/api/payments/{id}", payment.getId())
                        .header("Authorization", "Bearer " + registerChefToken()))
                .andExpect(status().isForbidden());

        assertThat(paymentRepository.existsById(payment.getId())).isTrue();
    }

    private FoodOrder seedOrder(String customerName,
                                String customerEmail,
                                String flatNumber,
                                String societyName,
                                BigDecimal totalAmount,
                                String status) {
        FoodOrder order = new FoodOrder();
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setFlatNumber(flatNumber);
        order.setSocietyName(societyName);
        order.setItems("Paneer Combo");
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setPaymentMethod("UPI");
        order.setOrderTime(LocalDateTime.now());
        order.setDiscount(BigDecimal.ZERO);
        order.setDeliveryCharge(BigDecimal.ZERO);
        return foodOrderRepository.save(order);
    }

    private Payment seedPayment(String residentName,
                                String flatNumber,
                                String residentEmail,
                                String societyName,
                                BigDecimal amount,
                                String status,
                                String paymentMethod,
                                Long orderId,
                                LocalDate paymentDate) {
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
        String email = "payment-chef+" + suffix + "@example.com";
        String flatNumber = "P-" + suffix.toUpperCase(Locale.ROOT);

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "Payment Chef",
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
}
