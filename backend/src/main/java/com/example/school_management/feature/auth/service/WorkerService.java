package com.example.school_management.feature.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.school_management.feature.auth.entity.Worker;
import com.example.school_management.feature.auth.dto.WorkerCreateDto;
import com.example.school_management.feature.auth.dto.WorkerUpdateDto;
import com.example.school_management.feature.auth.dto.WorkerDto;
import com.example.school_management.feature.auth.mapper.WorkerMapper;
import com.example.school_management.feature.auth.repository.WorkerRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;

@Service
public class WorkerService
        extends AbstractUserCrudService<
        Worker,
        WorkerCreateDto,
        WorkerUpdateDto,
        WorkerDto
        > {

    public WorkerService(WorkerRepository repo,
                         WorkerMapper mapper,
                         PasswordEncoder passwordEncoder,
                         PasswordUtil passwordUtil,
                         ApplicationEventPublisher events) {
        super(repo, mapper, passwordEncoder, passwordUtil, events);
    }
}