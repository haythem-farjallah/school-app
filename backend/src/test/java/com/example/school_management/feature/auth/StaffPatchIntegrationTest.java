package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.entity.enums.StaffType;
import com.example.school_management.feature.auth.repository.StaffRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /api/admin/staff/{id} merges the request into the staff member: fields the request
 * omits keep their stored values. Each test works on its own staff member.
 */
@IntegrationTest
class StaffPatchIntegrationTest {

    private static final LocalDateTime BIRTHDAY = LocalDateTime.of(1985, 6, 15, 0, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StaffRepository staffRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void departmentOnlyPatchKeepsEveryOtherField() throws Exception {
        Staff staff = saveStaff();

        patchStaff(staff.getId(), Map.of("department", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.department").value("IT"))
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.email").value(staff.getEmail()));

        Staff stored = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(stored.getDepartment()).isEqualTo("IT");
        assertProfileUnchanged(stored, staff);
        assertThat(stored.getStaffType()).isEqualTo(StaffType.ADMINISTRATIVE);
    }

    @Test
    void staffTypeOnlyPatchKeepsEveryOtherField() throws Exception {
        Staff staff = saveStaff();

        patchStaff(staff.getId(), Map.of("staffType", "SECURITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffType").value("SECURITY"))
                .andExpect(jsonPath("$.data.lastName").value("Staff"));

        Staff stored = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(stored.getStaffType()).isEqualTo(StaffType.SECURITY);
        assertProfileUnchanged(stored, staff);
        assertThat(stored.getDepartment()).isEqualTo("FINANCE");
    }

    @Test
    void partialProfilePatchKeepsTheOmittedProfileFields() throws Exception {
        Staff staff = saveStaff();

        patchStaff(staff.getId(), Map.of("profile", Map.of("address", "4 Server Road")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("4 Server Road"))
                .andExpect(jsonPath("$.data.telephone").value("555-0200"));

        Staff stored = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(stored.getAddress()).isEqualTo("4 Server Road");
        assertThat(stored.getFirstName()).isEqualTo("Sara");
        assertThat(stored.getLastName()).isEqualTo("Staff");
        assertThat(stored.getEmail()).isEqualTo(staff.getEmail());
        assertThat(stored.getTelephone()).isEqualTo("555-0200");
        assertThat(stored.getBirthday()).isEqualTo(BIRTHDAY);
        assertThat(stored.getGender()).isEqualTo("F");
        assertThat(stored.getStaffType()).isEqualTo(StaffType.ADMINISTRATIVE);
        assertThat(stored.getDepartment()).isEqualTo("FINANCE");
    }

    @Test
    void birthdayPatchIsStoredAndReturned() throws Exception {
        Staff staff = saveStaff();

        patchStaff(staff.getId(), Map.of("profile", Map.of("birthday", "1990-02-28")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birthday").value("1990-02-28T00:00:00"));

        Staff stored = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(stored.getBirthday()).isEqualTo(LocalDateTime.of(1990, 2, 28, 0, 0));
        assertThat(stored.getFirstName()).isEqualTo("Sara");
    }

    @Test
    void createStoresAndReturnsTheBirthday() throws Exception {
        String email = "staff-" + UUID.randomUUID() + "@fixtures.school.test";
        Map<String, Object> profile = Map.of(
                "firstName", "Nora",
                "lastName", "New",
                "email", email,
                "birthday", "1985-06-15",
                "gender", "F",
                "role", "STAFF");

        mockMvc.perform(post("/api/admin/staff")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "profile", profile, "staffType", "LIBRARIAN", "department", "LIBRARY"))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.birthday").value("1985-06-15T00:00:00"));

        Staff stored = staffRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getBirthday()).isEqualTo(BIRTHDAY);
    }

    private static void assertProfileUnchanged(Staff stored, Staff original) {
        assertThat(stored.getFirstName()).isEqualTo("Sara");
        assertThat(stored.getLastName()).isEqualTo("Staff");
        assertThat(stored.getEmail()).isEqualTo(original.getEmail());
        assertThat(stored.getTelephone()).isEqualTo("555-0200");
        assertThat(stored.getBirthday()).isEqualTo(BIRTHDAY);
        assertThat(stored.getGender()).isEqualTo("F");
        assertThat(stored.getAddress()).isEqualTo("3 Office Lane");
    }

    private Staff saveStaff() {
        Staff staff = new Staff();
        staff.setRole(UserRole.STAFF);
        staff.setStatus(Status.ACTIVE);
        staff.setEmail("staff-" + UUID.randomUUID() + "@fixtures.school.test");
        staff.setPassword("unused");
        staff.setFirstName("Sara");
        staff.setLastName("Staff");
        staff.setTelephone("555-0200");
        staff.setBirthday(BIRTHDAY);
        staff.setGender("F");
        staff.setAddress("3 Office Lane");
        staff.setStaffType(StaffType.ADMINISTRATIVE);
        staff.setDepartment("FINANCE");
        return staffRepository.save(staff);
    }

    private ResultActions patchStaff(long id, Map<String, Object> body) throws Exception {
        return mockMvc.perform(patch("/api/admin/staff/" + id)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String adminBearer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.10." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", DevFixtureLoader.ADMIN_EMAIL, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
