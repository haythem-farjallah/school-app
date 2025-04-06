package com.example.school_management;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestService {

    private final TestRepository testRepository;

    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public List<TestDTO> getAllTests() {
        List<TestEntity> entities = testRepository.findAll();
        return entities.stream()
                .map(entity -> new TestDTO(entity.getId(), entity.getName(), entity.getCreatedAt()))
                .collect(Collectors.toList());
    }
}