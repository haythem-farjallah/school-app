package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.AdministrationCreateDto;
import com.example.school_management.feature.auth.dto.AdministrationDto;
import com.example.school_management.feature.auth.dto.AdministrationUpdateDto;
import com.example.school_management.feature.auth.entity.Administration;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.mapper.AdministrationMapper;
import com.example.school_management.feature.auth.repository.AdministrationRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import com.example.school_management.feature.operational.service.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdministrationService extends AbstractUserCrudService<
        Administration,
        AdministrationCreateDto,
        AdministrationUpdateDto,
        AdministrationDto>{

    private final AdministrationMapper mapper;
    private final PasswordEncoder      passwordEncoder;
    private final AdministrationRepository administrationRepo;

    public AdministrationService(AdministrationRepository repo,
                                 AdministrationMapper mapper,
                                 PasswordEncoder enc,
                                 PasswordUtil pw,
                                 ApplicationEventPublisher events,
                                 AuditService auditService,
                                 UserRepository userRepository) {
        super(repo, mapper, enc, pw, events, auditService, userRepository);
        this.mapper = mapper;
        this.passwordEncoder = enc;
        this.administrationRepo = repo;
    }

    @Override
    public Administration create(AdministrationCreateDto dto) {
        Administration admin = mapper.toEntity(dto);

        /* ------ PASSWORD (mandatory) ------ */
        if (dto.password() == null || dto.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for admin account");
        }
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setPasswordChangeRequired(false);   // user picked the password

        /* ------ BASIC STATE ------ */
        admin.setStatus(Status.ACTIVE);

        /* ------ Persist and return ------ */
        return administrationRepo.save(admin);
    }
}
