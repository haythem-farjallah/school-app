package com.example.school_management;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    @Operation(summary = "Get test data", description = "Returns a list of test data from test_table")
    public ResponseEntity<List<TestDTO>> getTestData() {
        List<TestDTO> tests = testService.getAllTests();
        return ResponseEntity.ok(tests);
    }
}
