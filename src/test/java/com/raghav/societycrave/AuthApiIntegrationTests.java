package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

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

    @Test
    void protectedBusinessApisRequireJwtAndAcceptValidToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "raghav@example.com",
                                  "societyName": "Green Valley Residency",
                                  "password": "Society123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/food/available"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/complaints"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/chefs/society/Green Valley Residency"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/dev/admin/clear-non-resident-data"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/food/available")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chefs/society/Green Valley Residency")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/dev/admin/clear-non-resident-data")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void invalidAndExpiredJwtAreRejectedForProtectedApis() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        String expiredToken = Jwts.builder()
                .subject("raghav@example.com")
                .claims(java.util.Map.of(
                        "role", "Customer",
                        "societyName", "Green Valley Residency"
                ))
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(buildSigningKey(jwtSecret))
                .compact();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    private SecretKey buildSigningKey(String secret) throws Exception {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() >= 32) {
            try {
                secretBytes = Decoders.BASE64.decode(secret);
            } catch (IllegalArgumentException ignored) {
                secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        }
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secretBytes);
        return Keys.hmacShaKeyFor(hashed);
    }
}
