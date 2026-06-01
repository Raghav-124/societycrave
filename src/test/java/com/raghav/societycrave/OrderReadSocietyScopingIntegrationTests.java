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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderReadSocietyScopingIntegrationTests {

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
    void customerSeesOnlyOrdersFromJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Paneer Combo");
        seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Other Society Thali");

        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$[0].items").value("Paneer Combo"));
    }

    @Test
    void chefSeesOnlyOrdersFromJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Green Society Meal");
        seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Other Society Meal");

        String token = registerChefToken();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$[0].items").value("Green Society Meal"));
    }

    @Test
    void statusFilterReturnsOnlyMatchingStatusWithinJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Placed Green Order");
        seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "DELIVERED", "Delivered Green Order");
        seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Placed Other Order");

        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/status")
                        .param("status", "PLACED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$[0].status").value("PLACED"))
                .andExpect(jsonPath("$[0].items").value("Placed Green Order"));
    }

    @Test
    void sameSocietyOrderByIdReturnsOrder() throws Exception {
        FoodOrder greenOrder = seedOrder("Raghav Agrawal", "A-101", GREEN_SOCIETY, "PLACED", "Id Match Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", greenOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(greenOrder.getId()))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void crossSocietyOrderByIdReturnsForbidden() throws Exception {
        FoodOrder otherOrder = seedOrder("Other Resident", "B-202", OTHER_SOCIETY, "PLACED", "Forbidden Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", otherOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedOrderReadReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
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

    private String registerChefToken() throws Exception {
        String email = "order-read-chef+" + System.nanoTime() + "@example.com";
        String flatNumber = "Y-" + (100 + (int) (System.nanoTime() % 900));

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "Order Read Chef",
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
}
