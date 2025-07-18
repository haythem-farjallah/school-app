package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.mapper.TeacherMapper;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import com.example.school_management.feature.operational.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
@Transactional
public class TeacherService extends AbstractUserCrudService<
        Teacher, TeacherCreateDto, TeacherUpdateDto, TeacherDto> {

    private final TeacherRepository teacherRepo;

    public TeacherService(TeacherRepository repo,
                          TeacherMapper mapper,
                          PasswordEncoder enc,
                          PasswordUtil pw,
                          ApplicationEventPublisher ev,
                          AuditService auditService) {
        super(repo, mapper, enc, pw, ev, auditService);
        this.teacherRepo = repo;
    }

    /* ---------- Enhanced filtering methods ---------- */
    
    @Transactional(readOnly = true)
    public Page<Teacher> findAllWithFilters(Pageable pageable,
                                          String firstNameLike,
                                          String lastNameLike,
                                          String emailLike,
                                          String qualificationsLike,
                                          String subjectsTaughtLike,
                                          Integer availableHours,
                                          String schedulePreferencesLike) {
        
        Specification<Teacher> spec = Specification.where(null);
        
        if (firstNameLike != null && !firstNameLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("firstName")), "%" + firstNameLike.toLowerCase() + "%"));
        }
        
        if (lastNameLike != null && !lastNameLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("lastName")), "%" + lastNameLike.toLowerCase() + "%"));
        }
        
        if (emailLike != null && !emailLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("email")), "%" + emailLike.toLowerCase() + "%"));
        }
        
        if (qualificationsLike != null && !qualificationsLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("qualifications")), "%" + qualificationsLike.toLowerCase() + "%"));
        }
        
        if (subjectsTaughtLike != null && !subjectsTaughtLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("subjectsTaught")), "%" + subjectsTaughtLike.toLowerCase() + "%"));
        }
        
        if (availableHours != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("weeklyCapacity"), availableHours));
        }
        
        if (schedulePreferencesLike != null && !schedulePreferencesLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("schedulePreferences")), "%" + schedulePreferencesLike.toLowerCase() + "%"));
        }
        
        return teacherRepo.findAll(spec, pageable);
    }

    /* ---------- Advanced filtering method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Teacher> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<Teacher> spec = DynamicSpecificationBuilder.build(criteria);
        return teacherRepo.findAll(spec, pageable);
    }

    /* ---------- Search method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Teacher> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return teacherRepo.findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        Specification<Teacher> spec = (root, q, cb) -> 
            cb.or(
                cb.like(cb.lower(root.get("firstName")), searchTerm),
                cb.like(cb.lower(root.get("lastName")), searchTerm),
                cb.like(cb.lower(root.get("email")), searchTerm),
                cb.like(cb.lower(root.get("qualifications")), searchTerm),
                cb.like(cb.lower(root.get("subjectsTaught")), searchTerm)
            );
        
        return teacherRepo.findAll(spec, pageable);
    }
}