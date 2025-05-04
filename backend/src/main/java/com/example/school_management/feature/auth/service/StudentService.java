package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.*;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.mapper.StudentMapper;
import com.example.school_management.feature.auth.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepo;
    private final StudentMapper     mapper;
    private final PasswordEncoder   passwordEncoder;

    public StudentDto create(StudentCreateDto dto) {
        if (studentRepo.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        Student s = mapper.toEntity(dto);
        s.setPassword(passwordEncoder.encode(dto.getPassword()));
        Student saved = studentRepo.save(s);
        return mapper.toDto(saved);
    }

    public List<StudentDto> listAll() {
        return studentRepo.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public StudentDto getById(Long id) {
        Student s = studentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return mapper.toDto(s);
    }

    public StudentDto update(Long id, StudentUpdateDto dto) {
        Student s = studentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        mapper.updateFromDto(dto, s);
        Student updated = studentRepo.save(s);
        return mapper.toDto(updated);
    }

    public void delete(Long id) {
        if (!studentRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        studentRepo.deleteById(id);
    }
}
