package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class RequestErrorSemanticsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void missingAdministrationPasswordIsBadRequest() throws Exception {
        mvc.perform(post("/api/admin").with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"profile":{"firstName":"Sara","lastName":"Admin","email":"new-admin@example.test","role":"ADMIN"}}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Password is required for admin account"))
                .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.instance").value("/api/admin"));
    }

    @Test
    void unknownRoleAndUserPermissionCodesAreBadRequest() throws Exception {
        Long id = users.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow().getId();
        for (String path : List.of("/api/admin/permissions/roles/TEACHER", "/api/admin/permissions/users/" + id)) {
            mvc.perform(put(path).with(user(DevFixtureLoader.ADMIN_EMAIL).authorities(
                            new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("PERMISSIONS_MANAGE")))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"codes\":[\"UNKNOWN_PHASE_8D_PERMISSION\"]}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Unknown permission code(s)"))
                    .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.instance").value(path));
        }
    }

    @Test
    void invalidEnrollmentGradeLevelIsBadRequest() throws Exception {
        for (var route : List.of(post("/api/v1/enrollments/auto-enroll/grade/NOT_A_GRADE"))) {
            mvc.perform(route.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.instance").exists());
        }
    }
}
