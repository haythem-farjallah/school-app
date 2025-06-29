package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.mapper.TeacherMapper;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TeacherService extends AbstractUserCrudService<
        Teacher, TeacherCreateDto, TeacherUpdateDto, TeacherDto> {

    public TeacherService(TeacherRepository repo,
                          TeacherMapper mapper,
                          PasswordEncoder enc,
                          PasswordUtil pw,
                          ApplicationEventPublisher ev) {
        super(repo, mapper, enc, pw, ev);
    }
}