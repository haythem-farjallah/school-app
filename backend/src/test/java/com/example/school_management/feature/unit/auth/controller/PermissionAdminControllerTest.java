//package com.example.school_management.feature.unit.auth.controller;
//
//import com.example.school_management.commons.configs.JwtTokenProvider;
//import com.example.school_management.feature.auth.controller.PermissionAdminController;
//import com.example.school_management.feature.auth.service.CustomUserDetailsService;
//import com.example.school_management.feature.auth.service.PermissionService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.hamcrest.Matchers.containsInAnyOrder;
//
//import java.util.Set;
//
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(PermissionAdminController.class)
//@Import({PermissionService.class })
//class PermissionAdminControllerTest {
//
//    @Autowired
//    MockMvc mvc;
//    @MockitoBean
//    PermissionService svc;
//    @MockitoBean
//    JwtTokenProvider jwtTokenProvider;
//    @MockitoBean
//    CustomUserDetailsService uds;
//
//
//    @WithMockUser(roles = "ADMIN")
//    @Test
//    void catalogue_returns200() throws Exception {
//        when(svc.listAllCodes()).thenReturn(Set.of("A","B"));
//        mvc.perform(get("/api/admin/permissions"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", containsInAnyOrder("A", "B")));
//    }
//}
