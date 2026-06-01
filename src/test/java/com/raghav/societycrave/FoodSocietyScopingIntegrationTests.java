package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.repository.FoodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoodSocietyScopingIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Sunrise Apartments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodRepository foodRepository;

    @Test
    void authenticatedCustomerSeesOnlyFoodsFromOwnSociety() throws Exception {
        seedScopedFoods();
        String customerToken = loginCustomerToken();

        mockMvc.perform(get("/api/food")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].societyName", everyItem(is(GREEN_SOCIETY))));
    }

    @Test
    void authenticatedChefSeesOnlyAvailableFoodsFromOwnSociety() throws Exception {
        seedScopedFoods();
        String chefToken = loginChefToken();

        mockMvc.perform(get("/api/food/available")
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Green Available"))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY));
    }

    @Test
    void crossSocietyFoodQueryIsForbidden() throws Exception {
        seedScopedFoods();
        String customerToken = loginCustomerToken();

        mockMvc.perform(get("/api/food/society/available")
                        .queryParam("societyName", OTHER_SOCIETY)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedFoodReadReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/food"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void frontendCompatibleSocietyEndpointStillWorksWhenSocietyMatchesJwt() throws Exception {
        seedScopedFoods();
        String customerToken = loginCustomerToken();

        mockMvc.perform(get("/api/food/society/available")
                        .queryParam("societyName", GREEN_SOCIETY)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY));
    }

    @Test
    void chefScopedEndpointsUseJwtSocietyEvenWithoutExplicitSocietyPath() throws Exception {
        seedScopedFoods();
        String chefToken = loginChefToken();

        mockMvc.perform(get("/api/food/society/chef")
                        .queryParam("chefName", "Meera Joshi")
                        .queryParam("flatNumber", "B-204")
                        .queryParam("societyName", GREEN_SOCIETY)
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].societyName", everyItem(is(GREEN_SOCIETY))));

        mockMvc.perform(get("/api/food/chef/{chefName}", "Meera Joshi")
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].societyName", everyItem(is(GREEN_SOCIETY))));
    }

    private void seedScopedFoods() {
        foodRepository.deleteAll();

        foodRepository.save(food(
                "Green Available",
                "Meera Joshi",
                "B-204",
                "North Indian",
                GREEN_SOCIETY,
                true
        ));
        foodRepository.save(food(
                "Green Hidden",
                "Meera Joshi",
                "B-204",
                "North Indian",
                GREEN_SOCIETY,
                false
        ));
        foodRepository.save(food(
                "Other Available",
                "Arjun Singh",
                "C-303",
                "Punjabi",
                OTHER_SOCIETY,
                true
        ));
        foodRepository.save(food(
                "Other Same Name Available",
                "Meera Joshi",
                "D-404",
                "Punjabi",
                OTHER_SOCIETY,
                true
        ));
    }

    private Food food(String name,
                      String chefName,
                      String chefFlatNumber,
                      String chefCuisine,
                      String societyName,
                      boolean available) {
        Food food = new Food();
        food.setName(name);
        food.setChefName(chefName);
        food.setChefFlatNumber(chefFlatNumber);
        food.setChefCuisine(chefCuisine);
        food.setSocietyName(societyName);
        food.setDescription(name + " description");
        food.setPrice(new BigDecimal("120.00"));
        food.setCategory("Veg");
        food.setAvailable(available);
        return food;
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
