package com.example.school_management.feature.auth.controller;


import com.example.school_management.feature.auth.dto.CreateStudentWithParentsRequest;
import com.example.school_management.feature.auth.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/students")
    public ResponseEntity<Void> createStudentWithParents(
            @Valid @RequestBody CreateStudentWithParentsRequest req
    ) {
        adminService.createStudentWithParents(req);
        log.info("Admin created student '{}' with {} parent(s)",
                req.student().email(), req.parents().size());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}