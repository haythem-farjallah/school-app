package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.mapper.ParentMapper;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class ParentService extends AbstractUserCrudService<
        Parent, ParentCreateDto, ParentUpdateDto, ParentDto> {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public ParentService(ParentRepository repo,
                        ParentMapper mapper,
                        PasswordEncoder enc,
                        PasswordUtil pw,
                        ApplicationEventPublisher ev,
                        AuditService auditService,
                        StudentRepository studentRepository,
                        UserRepository userRepository) {
        super(repo, mapper, enc, pw, ev, auditService, userRepository);
        this.studentRepository = studentRepository;
        this.parentRepository = repo;
    }

    @Override
    public Parent create(ParentCreateDto dto) {
        log.debug("Creating parent with children emails: {}", dto.childrenEmails());
        
        // Create the parent first
        Parent parent = super.create(dto);
        
        // Assign children if emails are provided
        if (dto.childrenEmails() != null && !dto.childrenEmails().isEmpty()) {
            assignChildrenToParent(parent, dto.childrenEmails());
        }
        
        return parent;
    }
    
    @Override
    public Parent patch(long id, ParentUpdateDto dto) {
        log.debug("Updating parent {} with children emails: {}", id, dto.getChildren());
        
        // Update the parent first using the parent class method
        Parent parent = super.patch(id, dto);
        
        // Handle children assignment if provided
        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            assignChildrenToParent(parent, dto.getChildren());
        }
        
        return parent;
    }
    
    /* ---------- Enhanced filtering methods ---------- */
    
    @Transactional(readOnly = true)
    public Page<Parent> findAllWithFilters(Pageable pageable,
                                         String firstNameLike,
                                         String lastNameLike,
                                         String emailLike,
                                         String telephoneLike,
                                         String preferredContactMethodLike) {
        
        Specification<Parent> spec = Specification.where(null);
        
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
        
        if (telephoneLike != null && !telephoneLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("telephone")), "%" + telephoneLike.toLowerCase() + "%"));
        }
        
        if (preferredContactMethodLike != null && !preferredContactMethodLike.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("preferredContactMethod")), "%" + preferredContactMethodLike.toLowerCase() + "%"));
        }
        
        return parentRepository.findAll(spec, pageable);
    }

    /* ---------- Advanced filtering method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Parent> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<Parent> spec = DynamicSpecificationBuilder.build(criteria);
        return parentRepository.findAll(spec, pageable);
    }

    /* ---------- Search method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Parent> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return parentRepository.findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        Specification<Parent> spec = (root, q, cb) -> 
            cb.or(
                cb.like(cb.lower(root.get("firstName")), searchTerm),
                cb.like(cb.lower(root.get("lastName")), searchTerm),
                cb.like(cb.lower(root.get("email")), searchTerm),
                cb.like(cb.lower(root.get("telephone")), searchTerm)
            );
        
        return parentRepository.findAll(spec, pageable);
    }
    
    private void assignChildrenToParent(Parent parent, List<String> childrenEmails) {
        log.debug("Assigning {} children to parent {}", childrenEmails.size(), parent.getId());
        
        Set<Student> children = new HashSet<>();
        
        for (String email : childrenEmails) {
            studentRepository.findByEmail(email).ifPresentOrElse(
                student -> {
                    children.add(student);
                    log.debug("Found student with email {}: {} {}", email, student.getFirstName(), student.getLastName());
                },
                () -> log.warn("Student with email {} not found", email)
            );
        }
        
        if (!children.isEmpty()) {
            parent.setChildren(children);
            log.info("Assigned {} children to parent {}", children.size(), parent.getId());
        }
    }
} 