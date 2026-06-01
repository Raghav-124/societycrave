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
import java.util.Locale;
import java.util.UUID;
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
    void customerSeesOnlyOwnOrdersFromJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Paneer Combo");
        seedOrder("Same Society Other", "other.green@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Other Customer Thali");
        seedOrder("Other Resident", "other@example.com", "C-303", OTHER_SOCIETY, "PLACED", "Other Society Thali");

        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$[0].items").value("Paneer Combo"));
    }

    @Test
    void chefSeesOnlyOrdersFromJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Green Society Meal");
        seedOrder("Another Resident", "other.green@example.com", "B-202", GREEN_SOCIETY, "ACCEPTED", "Second Green Meal");
        seedOrder("Other Resident", "other@example.com", "C-303", OTHER_SOCIETY, "PLACED", "Other Society Meal");

        String token = registerChefToken();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customerStatusFilterReturnsOnlyOwnMatchingStatusWithinJwtSociety() throws Exception {
        seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Placed Green Order");
        seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "DELIVERED", "Delivered Green Order");
        seedOrder("Same Society Other", "other.green@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Placed Other Customer Order");
        seedOrder("Other Resident", "other@example.com", "C-303", OTHER_SOCIETY, "PLACED", "Placed Other Society Order");

        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/status")
                        .param("status", "PLACED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$[0].status").value("PLACED"))
                .andExpect(jsonPath("$[0].items").value("Placed Green Order"));
    }

    @Test
    void chefStatusFilterReturnsSameSocietyMatchingStatusOrders() throws Exception {
        seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Chef Placed Order");
        seedOrder("Same Society Other", "other.green@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Chef Other Same Society");
        seedOrder("Same Society Delivered", "other.green2@example.com", "D-404", GREEN_SOCIETY, "DELIVERED", "Delivered Green");
        seedOrder("Other Resident", "other@example.com", "C-303", OTHER_SOCIETY, "PLACED", "Other Society Order");
        String token = registerChefToken();

        mockMvc.perform(get("/api/orders/status")
                        .param("status", "PLACED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customerOwnOrderByIdReturnsOrder() throws Exception {
        FoodOrder greenOrder = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Id Match Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", greenOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(greenOrder.getId()))
                .andExpect(jsonPath("$.customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void customerSameSocietyOtherCustomerOrderByIdReturnsForbidden() throws Exception {
        FoodOrder otherOrder = seedOrder("Same Society Other", "other.green@example.com", "B-202", GREEN_SOCIETY, "PLACED", "Forbidden Same Society Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", otherOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerLegacyNullCustomerEmailOrderByIdReturnsForbidden() throws Exception {
        FoodOrder legacyOrder = seedOrder("Legacy Resident", null, "L-101", GREEN_SOCIETY, "PLACED", "Legacy Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", legacyOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossSocietyOrderByIdReturnsForbidden() throws Exception {
        FoodOrder otherOrder = seedOrder("Other Resident", "other@example.com", "B-202", OTHER_SOCIETY, "PLACED", "Forbidden Order");
        String token = loginCustomerToken();

        mockMvc.perform(get("/api/orders/{id}", otherOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefSameSocietyOrderByIdReturnsOrder() throws Exception {
        FoodOrder greenOrder = seedOrder("Raghav Agrawal", "raghav@example.com", "A-101", GREEN_SOCIETY, "PLACED", "Chef Id Match Order");
        String token = registerChefToken();

        mockMvc.perform(get("/api/orders/{id}", greenOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(greenOrder.getId()))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void unauthenticatedOrderReadReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
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

    private String registerChefToken() throws Exception {
        String suffix = uniqueSuffix();
        String email = "order-read-chef+" + suffix + "@example.com";
        String flatNumber = "Y-" + suffix.toUpperCase(Locale.ROOT);

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

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
