package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.mapper.ParentMapper;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParentService extends AbstractUserCrudService<
        Parent, ParentCreateDto, ParentUpdateDto, ParentDto> {

    public ParentService(ParentRepository repo,
                        ParentMapper mapper,
                        PasswordEncoder enc,
                        PasswordUtil pw,
                        ApplicationEventPublisher ev) {
        super(repo, mapper, enc, pw, ev);
    }
} 