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
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Old Items");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Fake Update",
                                  "customerEmail": "intruder@example.com",
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
                .andExpect(jsonPath("$.customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.items").value("Updated Items"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));
    }

    @Test
    void crossSocietyUpdateReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "other@example.com", "B-202", OTHER_SOCIETY, "PLACED", "Other Items");
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
    void customerCannotEditAnotherCustomersSameSocietyOrder() throws Exception {
        FoodOrder order = seedOrder("Another Resident", "another@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Another Order");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Raghav Agrawal",
                                  "customerEmail": "raghav@example.com",
                                  "flatNumber": "A-101",
                                  "societyName": "%s",
                                  "status": "PLACED",
                                  "items": "Take Over Attempt",
                                  "totalAmount": 210.00,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(GREEN_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotEditLegacyOrderWithNullCustomerEmail() throws Exception {
        FoodOrder order = seedOrder("Legacy Resident", null, "L-101", GREEN_SOCIETY, "PLACED", "Legacy Order");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Raghav Agrawal",
                                  "customerEmail": "raghav@example.com",
                                  "flatNumber": "A-101",
                                  "societyName": "%s",
                                  "status": "PLACED",
                                  "items": "Legacy Takeover Attempt",
                                  "totalAmount": 210.00,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(GREEN_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefCannotEditOrder() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Customer Order");
        String token = loginChefToken();

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Chef Intruder",
                                  "flatNumber": "B-202",
                                  "societyName": "%s",
                                  "status": "PLACED",
                                  "items": "Chef Edit Attempt",
                                  "totalAmount": 210.00,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(GREEN_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameSocietyUpdateCannotMoveOrderToAnotherSociety() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Same Society Items");
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
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Delete Me");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(foodOrderRepository.existsById(order.getId())).isFalse();
    }

    @Test
    void customerCannotDeleteAnotherCustomersSameSocietyOrder() throws Exception {
        FoodOrder order = seedOrder("Another Resident", "another@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Other Customer Order");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(foodOrderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void customerCannotDeleteLegacyOrderWithNullCustomerEmail() throws Exception {
        FoodOrder order = seedOrder("Legacy Resident", null, "L-101", GREEN_SOCIETY, "PLACED", "Legacy Delete");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(foodOrderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void chefCannotDeleteOrder() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Keep Me");
        String token = loginChefToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(foodOrderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void crossSocietyDeleteReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "other@example.com", "B-202", OTHER_SOCIETY, "PLACED", "Do Not Delete");
        String token = loginCustomerToken();

        mockMvc.perform(delete("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(foodOrderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void sameSocietyStatusUpdateSucceeds() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Status Me");
        String token = loginChefToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("acceptedBy", "Meera Joshi")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedBy").value("Meera Joshi"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void chefCanUpdateAcceptedOrderToDelivered() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "ACCEPTED", "Deliver Me");
        order.setAcceptedBy("Meera Joshi");
        foodOrderRepository.save(order);
        String token = loginChefToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.acceptedBy").value("Meera Joshi"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void customerCannotUpdateOrderToAccepted() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Status Me");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("acceptedBy", "Meera Joshi")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUpdateOrderToDelivered() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "ACCEPTED", "Status Me");
        order.setAcceptedBy("Meera Joshi");
        foodOrderRepository.save(order);
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "DELIVERED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanCancelPlacedOrder() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Cancel Me");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void customerCannotCancelAnotherCustomersSameSocietyOrder() throws Exception {
        FoodOrder order = seedOrder("Another Resident", "another@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Do Not Cancel");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCancelLegacyOrderWithNullCustomerEmail() throws Exception {
        FoodOrder order = seedOrder("Legacy Resident", null, "L-101", GREEN_SOCIETY, "PLACED", "Legacy Cancel");
        String token = loginCustomerToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefCannotCancelOrder() throws Exception {
        FoodOrder order = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Do Not Cancel");
        String token = loginChefToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELLED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossSocietyStatusUpdateReturnsForbidden() throws Exception {
        FoodOrder order = seedOrder("Other Resident", "other@example.com", "B-202", OTHER_SOCIETY, "PLACED", "Wrong Society Status");
        String token = loginChefToken();

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ACCEPTED")
                        .param("acceptedBy", "Meera Joshi"))
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
                                String customerEmail,
                                String flatNumber,
                                String societyName,
                                String status,
                                String items) {
        FoodOrder order = new FoodOrder();
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
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

    private String loginChefToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/chefs/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefCode": "CHEF-MEERA01",
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
