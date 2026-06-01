package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.entity.Food;
import com.raghav.societycrave.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoodMutationScopingIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Sunrise Apartments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodRepository foodRepository;

    @BeforeEach
    void setUp() {
        foodRepository.deleteAll();
    }

    @Test
    void sameSocietyFoodCreationSucceedsAndDerivesIdentityFromJwt() throws Exception {
        ChefRegistration chef = registerChef("Chef Create", "Punjabi", GREEN_SOCIETY);

        mockMvc.perform(post("/api/food")
                        .header("Authorization", "Bearer " + chef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Paneer Roll",
                                  "chefName": "Wrong Name",
                                  "chefFlatNumber": "Z-999",
                                  "chefCuisine": "Wrong Cuisine",
                                  "description": "Fresh paneer",
                                  "price": 180.00,
                                  "category": "Dinner",
                                  "availableDays": "Mon, Tue",
                                  "openingTime": "18:00",
                                  "closingTime": "21:00",
                                  "slidingWindowMinutes": 30,
                                  "available": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.chefName").value(chef.displayName()))
                .andExpect(jsonPath("$.chefFlatNumber").value(chef.flatNumber()))
                .andExpect(jsonPath("$.chefCuisine").value(chef.cuisine()));
    }

    @Test
    void crossSocietyFoodCreationRequestReturnsForbidden() throws Exception {
        ChefRegistration chef = registerChef("Chef Forbidden", "North Indian", GREEN_SOCIETY);

        mockMvc.perform(post("/api/food")
                        .header("Authorization", "Bearer " + chef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Wrong Society Dish",
                                  "societyName": "%s",
                                  "description": "Should fail",
                                  "price": 120.00
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameSocietyFoodUpdateSucceedsAndKeepsTrustedChefFields() throws Exception {
        ChefRegistration chef = registerChef("Chef Update", "Gujarati", GREEN_SOCIETY);
        Food food = seedFood("Thepla", chef.displayName(), chef.flatNumber(), chef.cuisine(), GREEN_SOCIETY);

        mockMvc.perform(put("/api/food/{id}", food.getId())
                        .header("Authorization", "Bearer " + chef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Stuffed Thepla",
                                  "chefName": "Other Chef",
                                  "chefFlatNumber": "Y-888",
                                  "chefCuisine": "Wrong Cuisine",
                                  "societyName": "%s",
                                  "description": "Updated dish",
                                  "price": 210.00,
                                  "category": "Breakfast",
                                  "availableDays": "Wed, Thu",
                                  "openingTime": "08:00",
                                  "closingTime": "11:00",
                                  "slidingWindowMinutes": 20,
                                  "available": false,
                                  "imageUrl": "image-data"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Stuffed Thepla"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.chefName").value(chef.displayName()))
                .andExpect(jsonPath("$.chefFlatNumber").value(chef.flatNumber()))
                .andExpect(jsonPath("$.chefCuisine").value(chef.cuisine()))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void crossSocietyFoodUpdateReturnsForbidden() throws Exception {
        ChefRegistration chef = registerChef("Chef Green", "South Indian", GREEN_SOCIETY);
        ChefRegistration otherSocietyChef = registerChef("Chef Other", "Punjabi", OTHER_SOCIETY);
        Food food = seedFood("Other Dish", otherSocietyChef.displayName(), otherSocietyChef.flatNumber(), otherSocietyChef.cuisine(), OTHER_SOCIETY);

        mockMvc.perform(put("/api/food/{id}", food.getId())
                        .header("Authorization", "Bearer " + chef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Attempted Update",
                                  "description": "No access",
                                  "price": 150.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameSocietyDifferentChefCannotUpdateFood() throws Exception {
        ChefRegistration ownerChef = registerChef("Owner Chef", "North Indian", GREEN_SOCIETY);
        ChefRegistration otherChef = registerChef("Other Chef", "Chinese", GREEN_SOCIETY);
        Food food = seedFood("Owner Dish", ownerChef.displayName(), ownerChef.flatNumber(), ownerChef.cuisine(), GREEN_SOCIETY);

        mockMvc.perform(put("/api/food/{id}", food.getId())
                        .header("Authorization", "Bearer " + otherChef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hijacked Dish",
                                  "description": "Should fail",
                                  "price": 199.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void sameSocietyFoodDeleteSucceeds() throws Exception {
        ChefRegistration chef = registerChef("Chef Delete", "Bengali", GREEN_SOCIETY);
        Food food = seedFood("Delete Dish", chef.displayName(), chef.flatNumber(), chef.cuisine(), GREEN_SOCIETY);

        mockMvc.perform(delete("/api/food/{id}", food.getId())
                        .header("Authorization", "Bearer " + chef.token()))
                .andExpect(status().isNoContent());

        assertThat(foodRepository.existsById(food.getId())).isFalse();
    }

    @Test
    void crossSocietyFoodDeleteReturnsForbidden() throws Exception {
        ChefRegistration chef = registerChef("Chef Green Delete", "Mughlai", GREEN_SOCIETY);
        ChefRegistration otherSocietyChef = registerChef("Chef Other Delete", "Thai", OTHER_SOCIETY);
        Food food = seedFood("Protected Dish", otherSocietyChef.displayName(), otherSocietyChef.flatNumber(), otherSocietyChef.cuisine(), OTHER_SOCIETY);

        mockMvc.perform(delete("/api/food/{id}", food.getId())
                        .header("Authorization", "Bearer " + chef.token()))
                .andExpect(status().isForbidden());

        assertThat(foodRepository.existsById(food.getId())).isTrue();
    }

    @Test
    void missingFoodUpdateAndDeleteReturnNotFound() throws Exception {
        ChefRegistration chef = registerChef("Chef Missing", "Fusion", GREEN_SOCIETY);

        mockMvc.perform(put("/api/food/{id}", 999999L)
                        .header("Authorization", "Bearer " + chef.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing Dish",
                                  "description": "Missing",
                                  "price": 100.00
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/food/{id}", 999999L)
                        .header("Authorization", "Bearer " + chef.token()))
                .andExpect(status().isNotFound());
    }

    private ChefRegistration registerChef(String displayName, String cuisine, String societyName) throws Exception {
        String email = displayName.toLowerCase().replace(" ", ".") + "+" + System.nanoTime() + "@example.com";
        String flatNumber = "F-" + (100 + (int) (System.nanoTime() % 900));

        MvcResult result = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "%s",
                                  "email": "%s",
                                  "chefCuisine": "%s",
                                  "flatNumber": "%s",
                                  "societyName": "%s",
                                  "password": "Society123"
                                }
                                """.formatted(displayName, email, cuisine, flatNumber, societyName)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new ChefRegistration(
                json.get("accessToken").asText(),
                displayName,
                flatNumber,
                cuisine,
                societyName
        );
    }

    private Food seedFood(String name,
                          String chefName,
                          String chefFlatNumber,
                          String chefCuisine,
                          String societyName) {
        Food food = new Food();
        food.setName(name);
        food.setChefName(chefName);
        food.setChefFlatNumber(chefFlatNumber);
        food.setChefCuisine(chefCuisine);
        food.setSocietyName(societyName);
        food.setDescription(name + " description");
        food.setPrice(new BigDecimal("140.00"));
        food.setCategory("Lunch");
        food.setAvailableDays("Mon, Tue");
        food.setOpeningTime("12:00");
        food.setClosingTime("15:00");
        food.setSlidingWindowMinutes(30);
        food.setAvailable(true);
        return foodRepository.save(food);
    }

    private record ChefRegistration(String token,
                                    String displayName,
                                    String flatNumber,
                                    String cuisine,
                                    String societyName) {
    }
}
