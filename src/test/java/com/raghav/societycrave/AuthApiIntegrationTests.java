package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void customerRegisterLoginAndMeReturnJwtAndLegacyProfileFields() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Api Resident",
                                  "email": "api.resident@example.com",
                                  "flatNumber": "E-101",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("Customer"))
                .andExpect(jsonPath("$.displayName").value("Api Resident"))
                .andExpect(jsonPath("$.email").value("api.resident@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("E-101"))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String registerToken = registerJson.get("accessToken").asText();
        assertThat(registerToken).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Customer"))
                .andExpect(jsonPath("$.displayName").value("Api Resident"))
                .andExpect(jsonPath("$.email").value("api.resident@example.com"))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"));

        mockMvc.perform(post("/api/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "api.resident@example.com",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("Customer"))
                .andExpect(jsonPath("$.displayName").value("Api Resident"))
                .andExpect(jsonPath("$.email").value("api.resident@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("E-101"))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"));
    }

    @Test
    void chefRegisterLoginAndMeReturnJwtAndLegacyProfileFields() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/chefs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefName": "Api Chef",
                                  "email": "api.chef@example.com",
                                  "chefCuisine": "Punjabi",
                                  "flatNumber": "F-202",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("Chef"))
                .andExpect(jsonPath("$.displayName").value("Api Chef"))
                .andExpect(jsonPath("$.email").value("api.chef@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("F-202"))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"))
                .andExpect(jsonPath("$.chefCuisine").value("Punjabi"))
                .andExpect(jsonPath("$.chefCode").value(org.hamcrest.Matchers.startsWith("CHEF-")))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String registerToken = registerJson.get("accessToken").asText();
        String chefCode = registerJson.get("chefCode").asText();
        assertThat(registerToken).isNotBlank();
        assertThat(chefCode).startsWith("CHEF-");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Chef"))
                .andExpect(jsonPath("$.displayName").value("Api Chef"))
                .andExpect(jsonPath("$.chefCode").value(chefCode))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"));

        mockMvc.perform(post("/api/auth/chefs/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chefCode": "%s",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """.formatted(chefCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("Chef"))
                .andExpect(jsonPath("$.displayName").value("Api Chef"))
                .andExpect(jsonPath("$.email").value("api.chef@example.com"))
                .andExpect(jsonPath("$.flatNumber").value("F-202"))
                .andExpect(jsonPath("$.societyName").value("Green Valley Residency"))
                .andExpect(jsonPath("$.chefCuisine").value("Punjabi"))
                .andExpect(jsonPath("$.chefCode").value(chefCode));
    }

    @Test
    void meRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
