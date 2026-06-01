package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.entity.FoodOrder;
import com.raghav.societycrave.repository.FoodOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderMutationSocietyScopingIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Other Society";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @BeforeEach
    void setUp() {
        foodOrderRepository.deleteAll();
    }

    @Test
    void sameSocietyUpdateSucceedsAndKeepsTrustedIdentityFields() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Old Items");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Fake Update",
                                  "flatNumber": "X-999",
                                  "societyName": "%s",
                                  "items": "Updated Items",
                                  "totalAmount": 255.00,
                                  "status": "PLACED",
                                  "discount": 10.00,
                                  "deliveryCharge": 25.00,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.items").value("Updated Items"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));
    }

    @Test
    void crossSocietyUpdateReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Other Items");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Intruder",
                                  "flatNumber": "Z-999",
                                  "societyName": "%s",
                                  "items": "Attempted Update",
                                  "totalAmount": 199.00,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameSocietyUpdateCannotMoveOrderToAnotherSociety() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Same Society Items");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Wrong Name",
                                  "flatNumber": "Y-111",
                                  "societyName": "%s",
                                  "items": "Still Green Society",
                                  "totalAmount": 299.00,
                                  "paymentMethod": "CASH"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void sameSocietyDeleteSucceeds() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Delete Me");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(foodOrderRepository.existsById(order.getId())).isFalse();
    }

    @Test
    void crossSocietyDeleteReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Do Not Delete");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(foodOrderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void sameSocietyStatusUpdateSucceeds() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Status Me");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void crossSocietyStatusUpdateReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Wrong Society Status");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingOrderMutationReturnsNotFound() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isNotFound());
    }

    private FoodOrder seedOrder(String customerName,
                                String flatNumber,
                                String societyName,
                                String status,
                                String items) {
        FoodOrder order = new FoodOrder();
        order.setCustomerName(customerName);
        order.setFlatNumber(flatNumber);
        order.setSocietyName(societyName);
        order.setStatus(status);
        order.setItems(items);
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setPaymentMethod("UPI");
        order.setOrderTime(LocalDateTime.now());
        order.setDiscount(BigDecimal.ZERO);
        order.setDeliveryCharge(BigDecimal.ZERO);
        return foodOrderRepository.save(order);
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
}
