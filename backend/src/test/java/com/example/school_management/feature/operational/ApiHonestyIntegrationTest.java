package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class ApiHonestyIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void assignmentCrudHasNoEndpoint() throws Exception {
        for (var route : List.of(get("/api/v1/assignments"), get("/api/v1/assignments/1"),
                post("/api/v1/assignments"), put("/api/v1/assignments/1"), delete("/api/v1/assignments/1"))) {
            mvc.perform(route.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("No endpoint matches this request"))
                    .andExpect(jsonPath("$.instance").exists());
        }
    }

    @Test
    void teachingAssignmentApiRemainsMapped() throws Exception {
        mvc.perform(get("/admin/teaching-assignments").with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").exists());
    }
}
