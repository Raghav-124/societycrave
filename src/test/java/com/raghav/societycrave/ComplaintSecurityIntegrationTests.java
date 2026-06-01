package com.raghav.societycrave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.societycrave.entity.Complaint;
import com.raghav.societycrave.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintSecurityIntegrationTests {

    private static final String GREEN_SOCIETY = "Green Valley Residency";
    private static final String OTHER_SOCIETY = "Other Society";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplaintRepository complaintRepository;

    @BeforeEach
    void setUp() {
        complaintRepository.deleteAll();
    }

    @Test
    void customerCanCreateComplaint() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Water leakage",
                                  "description": "There is a pipeline leak near A wing."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Water leakage"))
                .andExpect(jsonPath("$.residentName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").doesNotExist());
    }

    @Test
    void complaintCreationIgnoresBodyResidentEmailAndStoresJwtIdentity() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lift issue",
                                  "description": "Lift button is not working.",
                                  "residentName": "Fake Resident",
                                  "flatNumber": "X-999",
                                  "residentEmail": "fake@example.com",
                                  "societyName": "%s",
                                  "status": "RESOLVED",
                                  "assignedTo": "Someone"
                                }
                                """.formatted(GREEN_SOCIETY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.residentName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").isEmpty());
    }

    @Test
    void complaintCreationRejectsMismatchedBodySociety() throws Exception {
        String token = loginCustomerToken();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Noise complaint",
                                  "description": "Loud noise at midnight.",
                                  "societyName": "%s"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefCannotCreateComplaint() throws Exception {
        String token = loginChefToken();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Chef complaint",
                                  "description": "Should not be allowed."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerGetsOnlyOwnComplaints() throws Exception {
        seedComplaint("Own Complaint", "My issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);
        seedComplaint("Other Same Society", "Another issue", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "OPEN", null, null);
        seedComplaint("Other Society", "Far away issue", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$[0].societyName").value(GREEN_SOCIETY));
    }

    @Test
    void customerGetsOnlyOwnMatchingStatusComplaints() throws Exception {
        seedComplaint("Own Open", "My open issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);
        seedComplaint("Own Resolved", "My resolved issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "RESOLVED", "Meera Joshi", LocalDateTime.now());
        seedComplaint("Other Open", "Other issue", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/status")
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Own Open"))
                .andExpect(jsonPath("$[0].residentEmail").value("raghav@example.com"));
    }

    @Test
    void customerGetsOwnComplaintById() throws Exception {
        Complaint complaint = seedComplaint("Own Complaint", "Own issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaint.getId()))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"));
    }

    @Test
    void customerCannotGetAnotherResidentsSameSocietyComplaint() throws Exception {
        Complaint complaint = seedComplaint("Other Complaint", "Other issue", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotGetLegacyNullResidentEmailComplaint() throws Exception {
        Complaint complaint = seedComplaint("Legacy Complaint", "Legacy issue", "Legacy Resident", "L-101", null, GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefGetsSameSocietyComplaints() throws Exception {
        seedComplaint("Green One", "Issue one", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);
        seedComplaint("Green Two", "Issue two", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "IN_PROGRESS", "Meera Joshi", null);
        seedComplaint("Other Society", "Issue elsewhere", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + loginChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void chefGetsSameSocietyMatchingStatusComplaints() throws Exception {
        seedComplaint("Green Open", "Issue one", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);
        seedComplaint("Green Resolved", "Issue two", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "RESOLVED", "Meera Joshi", LocalDateTime.now());
        seedComplaint("Other Open", "Issue elsewhere", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/status")
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + loginChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Green Open"));
    }

    @Test
    void chefGetsSameSocietyComplaintById() throws Exception {
        Complaint complaint = seedComplaint("Green One", "Issue one", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginChefToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaint.getId()));
    }

    @Test
    void chefCrossSocietyComplaintReadReturnsForbidden() throws Exception {
        Complaint complaint = seedComplaint("Other Society", "Issue elsewhere", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(get("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginChefToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanUpdateOwnComplaint() throws Exception {
        Complaint complaint = seedComplaint("Own Complaint", "Original issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(put("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Complaint",
                                  "description": "Updated details",
                                  "residentName": "Fake Resident",
                                  "flatNumber": "X-999",
                                  "residentEmail": "fake@example.com",
                                  "societyName": "%s",
                                  "status": "RESOLVED",
                                  "assignedTo": "Someone"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Complaint"))
                .andExpect(jsonPath("$.description").value("Updated details"))
                .andExpect(jsonPath("$.residentName").value("Raghav Agrawal"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").isEmpty());
    }

    @Test
    void customerCannotUpdateAnotherResidentsComplaint() throws Exception {
        Complaint complaint = seedComplaint("Other Complaint", "Other issue", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(put("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Takeover",
                                  "description": "Should fail"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUpdateLegacyNullResidentEmailComplaint() throws Exception {
        Complaint complaint = seedComplaint("Legacy Complaint", "Legacy issue", "Legacy Resident", "L-101", null, GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(put("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Takeover",
                                  "description": "Should fail"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefCanUpdateSameSocietyComplaintOperationalFields() throws Exception {
        Complaint complaint = seedComplaint("Chef Complaint", "Issue", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(put("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginChefToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ignored Title",
                                  "description": "Ignored Description",
                                  "residentName": "Fake Resident",
                                  "flatNumber": "X-999",
                                  "residentEmail": "fake@example.com",
                                  "societyName": "%s",
                                  "status": "RESOLVED",
                                  "assignedTo": "Meera Joshi"
                                }
                                """.formatted(OTHER_SOCIETY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Chef Complaint"))
                .andExpect(jsonPath("$.description").value("Issue"))
                .andExpect(jsonPath("$.residentEmail").value("raghav@example.com"))
                .andExpect(jsonPath("$.societyName").value(GREEN_SOCIETY))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.assignedTo").value("Meera Joshi"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    @Test
    void crossSocietyChefComplaintUpdateReturnsForbidden() throws Exception {
        Complaint complaint = seedComplaint("Other Society", "Issue elsewhere", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(put("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginChefToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cross Society Attempt",
                                  "description": "Should be blocked",
                                  "status": "RESOLVED",
                                  "assignedTo": "Meera Joshi"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanDeleteOwnComplaint() throws Exception {
        Complaint complaint = seedComplaint("Own Complaint", "Delete me", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(delete("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isNoContent());

        assertThat(complaintRepository.existsById(complaint.getId())).isFalse();
    }

    @Test
    void customerCannotDeleteAnotherResidentsComplaint() throws Exception {
        Complaint complaint = seedComplaint("Other Complaint", "Delete me", "Other Resident", "B-202", "other.green@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(delete("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());

        assertThat(complaintRepository.existsById(complaint.getId())).isTrue();
    }

    @Test
    void customerCannotDeleteLegacyNullResidentEmailComplaint() throws Exception {
        Complaint complaint = seedComplaint("Legacy Complaint", "Delete me", "Legacy Resident", "L-101", null, GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(delete("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());

        assertThat(complaintRepository.existsById(complaint.getId())).isTrue();
    }

    @Test
    void chefDeleteReturnsForbidden() throws Exception {
        Complaint complaint = seedComplaint("Chef Complaint", "Do not delete", "Raghav Agrawal", "A-101", "raghav@example.com", GREEN_SOCIETY, "OPEN", null, null);

        mockMvc.perform(delete("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginChefToken()))
                .andExpect(status().isForbidden());

        assertThat(complaintRepository.existsById(complaint.getId())).isTrue();
    }

    @Test
    void crossSocietyCustomerComplaintDeleteReturnsForbidden() throws Exception {
        Complaint complaint = seedComplaint("Other Society", "Do not delete", "Other Resident", "C-303", "other@example.com", OTHER_SOCIETY, "OPEN", null, null);

        mockMvc.perform(delete("/api/complaints/{id}", complaint.getId())
                        .header("Authorization", "Bearer " + loginCustomerToken()))
                .andExpect(status().isForbidden());
    }

    private Complaint seedComplaint(String title,
                                    String description,
                                    String residentName,
                                    String flatNumber,
                                    String residentEmail,
                                    String societyName,
                                    String status,
                                    String assignedTo,
                                    LocalDateTime resolvedAt) {
        Complaint complaint = new Complaint();
        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setResidentName(residentName);
        complaint.setFlatNumber(flatNumber);
        complaint.setResidentEmail(residentEmail);
        complaint.setSocietyName(societyName);
        complaint.setStatus(status);
        complaint.setAssignedTo(assignedTo);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setResolvedAt(resolvedAt);
        return complaintRepository.save(complaint);
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
