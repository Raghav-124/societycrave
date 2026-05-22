package com.raghav.societycrave;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebAccessIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsJson() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("SocietyCrave"));
    }

    @Test
    void staticFrontendLoadsWithoutSecurityRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SocietyCrave")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));
    }
}
