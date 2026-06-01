package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.repository.FoodOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderCreationIdentityIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Other Society";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @BeforeEach
    void clearOrders() {
        foodOrderRepository.deleteAll();
    }

    @Test
    void orderCreationUsesJwtSocietyWhenBodyOmitsSociety() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": "Dal, Rice",
                                  "totalAmount": 180.00,
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void orderCreationRejectsMismatchedBodySociety() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Wrong Name",
                                  "flatNumber": "Z-999",
                                  "societyName": "%s",
                                  "items": "Dal, Rice",
                                  "totalAmount": 180.00,
                                  "paymentMethod": "UPI"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderCreationUsesJwtDisplayNameAndFlatNumber() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Fake Customer",
                                  "customerEmail": "fake@example.com",
                                  "flatNumber": "X-404",
                                  "societyName": "%s",
                                  "items": "Roti, Paneer",
                                  "totalAmount": 220.00,
                                  "paymentMethod": "CASH"
                                }
                                """.formatted(GREEN_SOCIETY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.customerEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY));
    }

    @Test
    void unauthenticatedOrderCreationReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": "Dal, Rice",
                                  "totalAmount": 180.00,
                                  "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void chefTokenCannotCreateOrder() throws Exception {
        ChefTokenRegistration chefRegistration = registerChefToken();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + chefRegistration.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Wrong Chef",
                                  "flatNumber": "Y-101",
                                  "items": "Thali",
                                  "totalAmount": 250.00,
                                  "paymentMethod": "CARD"
                                }
                                """))
                .andExpect(status().isForbidden());
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

    private ChefTokenRegistration registerChefToken() throws Exception {
        String suffix = uniqueSuffix();
        String email = "order-chef+" + suffix + "@example.com";
        String flatNumber = "Z-" + suffix.toUpperCase(Locale.ROOT);
        String displayName = "Order Chef";

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "%s",
                                  "email": "%s",
                                  "chefCuisine": "Punjabi",
                                  "flatNumber": "%s",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """.formatted(displayName, email, flatNumber)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new ChefTokenRegistration(
                json.get("accessToken").asText(),
                displayName,
                flatNumber
        );
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record ChefTokenRegistration(String token, String displayName, String flatNumber) {
    }
}
