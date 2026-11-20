package com.aegiscare.security;

import com.aegiscare.entity.Hospital;
import com.aegiscare.entity.Patient;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.entity.UserAccount;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.PatientRepository;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserAccountRepository accounts;
    @Autowired HospitalRepository hospitals;
    @Autowired PatientRepository patients;
    @Autowired TriageSessionRepository sessions;
    @Autowired PasswordEncoder passwordEncoder;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private UserAccount coordinator;

    @BeforeEach
    void prepareScopedTestData() {
        var existingHospitals = hospitals.findAll();
        if (existingHospitals.isEmpty()) {
            hospitalA = hospitals.save(hospital("Facility Alpha", 100, 45));
            hospitalB = hospitals.save(hospital("Facility Beta", 80, 20));
        } else {
            hospitalA = existingHospitals.get(0);
            hospitalB = existingHospitals.size() > 1 ? existingHospitals.get(1) : hospitals.save(hospital("Facility Beta", 80, 20));
        }

        coordinator = accounts.findByEmailIgnoreCase("coordinator@test.aegiscare.local").orElseGet(UserAccount::new);
        coordinator.setEmail("coordinator@test.aegiscare.local");
        coordinator.setFullName("Test Coordinator");
        coordinator.setPasswordHash(passwordEncoder.encode("TestCoordinatorPassword2026!"));
        coordinator.setRole(UserRole.COORDINATOR);
        coordinator.setHospitalId(hospitalA.getId());
        coordinator.setEnabled(true);
        coordinator = accounts.save(coordinator);

        UserAccount clinician = accounts.findByEmailIgnoreCase("clinician@test.aegiscare.local").orElseGet(UserAccount::new);
        clinician.setEmail("clinician@test.aegiscare.local");
        clinician.setFullName("Test Clinician");
        clinician.setPasswordHash(passwordEncoder.encode("TestClinicianPassword2026!"));
        clinician.setRole(UserRole.CLINICIAN);
        clinician.setHospitalId(hospitalA.getId());
        clinician.setEnabled(true);
        accounts.save(clinician);

        Patient patientB = patients.findByPatientCode("PATIENT-B-ONLY").orElseGet(Patient::new);
        patientB.setPatientCode("PATIENT-B-ONLY");
        patientB.setFullName("Synthetic Patient Beta");
        patientB.setHospitalId(hospitalB.getId());
        patients.save(patientB);

        TriageSession remoteCase = sessions.findAll().stream()
                .filter(session -> "PATIENT-B-HIGH-RISK".equals(session.getPatientCode()))
                .findFirst().orElseGet(TriageSession::new);
        remoteCase.setPatientCode("PATIENT-B-HIGH-RISK");
        remoteCase.setHospitalId(hospitalB.getId());
        remoteCase.setSymptomsCsv("synthetic test data");
        remoteCase.setSeverity("critical");
        remoteCase.setRiskScore(96);
        remoteCase.setEscalationNeeded(true);
        remoteCase.setStatus("escalated");
        sessions.save(remoteCase);
    }

    @Test
    void protectedWorkspaceRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanLoginAndReadOwnProfile() throws Exception {
        String token = login("admin@test.aegiscare.local", "TestAdminPassword2026!");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@test.aegiscare.local"));
    }

    @Test
    void coordinatorIsScopedToAssignedHospital() throws Exception {
        String token = login("coordinator@test.aegiscare.local", "TestCoordinatorPassword2026!");
        mvc.perform(get("/api/v1/dashboard/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitals_tracked").value(1));
    }

    @Test
    void clinicianCannotCreateATriageSessionForAnotherHospitalsPatient() throws Exception {
        String token = login("clinician@test.aegiscare.local", "TestClinicianPassword2026!");
        mvc.perform(post("/api/v1/ai/analyze")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"PATIENT-B-ONLY\",\"symptoms\":[\"fever\"],\"duration\":\"1 day\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void coordinatorCannotAccessAdminUserManagement() throws Exception {
        String token = login("coordinator@test.aegiscare.local", "TestCoordinatorPassword2026!");
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void disablingAnAccountInvalidatesItsExistingToken() throws Exception {
        String coordinatorToken = login("coordinator@test.aegiscare.local", "TestCoordinatorPassword2026!");
        String adminToken = login("admin@test.aegiscare.local", "TestAdminPassword2026!");
        mvc.perform(patch("/api/v1/admin/users/{id}/disable", coordinator.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/dashboard/summary").header("Authorization", bearer(coordinatorToken)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void coordinatorCannotRunClinicalTriage() throws Exception {
        String token = login("coordinator@test.aegiscare.local", "TestCoordinatorPassword2026!");
        mvc.perform(post("/api/v1/ai/analyze")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"COORDINATOR-ATTEMPT\",\"symptoms\":[\"fever\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinatorCannotSeeAnotherHospitalsEmergencyQueue() throws Exception {
        String token = login("coordinator@test.aegiscare.local", "TestCoordinatorPassword2026!");
        mvc.perform(get("/api/v1/emergency/high-risk")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("PATIENT-B-HIGH-RISK"))));
    }

    @Test
    void invalidCredentialsDoNotAuthenticate() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.aegiscare.local\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("accessToken").asText();
    }

    private static String bearer(String token) { return "Bearer " + token; }

    private Hospital hospital(String name, int beds, int occupied) {
        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setCity("Test City");
        hospital.setTotalBeds(beds);
        hospital.setCurrentOccupancy(occupied);
        hospital.setErCapacity(20);
        hospital.setErOccupancy(5);
        hospital.setAmbulances(1);
        hospital.setHasEmergency(true);
        hospital.setHasIcu(true);
        return hospital;
    }

    private record LoginPayload(String email, String password) { }
}
