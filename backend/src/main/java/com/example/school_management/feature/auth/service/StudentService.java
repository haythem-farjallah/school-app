package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.mapper.StudentMapper;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StudentService extends AbstractUserCrudService<
        Student, StudentCreateDto, StudentUpdateDto, StudentDto> {

    public StudentService(StudentRepository repo,
                         StudentMapper mapper,
                         PasswordEncoder enc,
                         PasswordUtil pw,
                         ApplicationEventPublisher ev) {
        super(repo, mapper, enc, pw, ev);
    }
}
