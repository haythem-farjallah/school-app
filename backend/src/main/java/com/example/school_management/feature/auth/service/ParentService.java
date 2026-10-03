package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.mapper.ParentMapper;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.service.SchoolMembershipProvisioningService;
import com.example.school_management.feature.auth.util.PasswordUtil;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.auth.repository.PeopleDirectorySpecifications;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
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

    /** Paths accepted by GET /api/admin/parent-management/filter. */
    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("firstName", "lastName", "email", "telephone", "preferredContactMethod", "relation"),
            Set.of("firstName", "lastName", "email"));

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final CurrentSchoolResolver currentSchoolResolver;

    public ParentService(ParentRepository repo,
                        ParentMapper mapper,
                        PasswordEncoder enc,
                        PasswordUtil pw,
                        ApplicationEventPublisher ev,
                        AuditService auditService,
                        StudentRepository studentRepository,
                        UserRepository userRepository,
                        SchoolMembershipProvisioningService membershipProvisioner,
                        CurrentSchoolResolver currentSchoolResolver) {
        super(repo, mapper, enc, pw, ev, auditService, userRepository, membershipProvisioner);
        this.studentRepository = studentRepository;
        this.parentRepository = repo;
        this.currentSchoolResolver = currentSchoolResolver;
    }

    @Override
    public Parent create(ParentCreateDto dto) {
        log.debug("Creating parent with {} children", dto.childrenEmails() == null ? 0 : dto.childrenEmails().size());
        
        Set<Student> children = resolveChildren(dto.childrenEmails());
        Parent parent = super.create(dto);
        if (!children.isEmpty()) {
            parent.setChildren(children);
        }

        return parent;
    }
    
    @Override
    public Parent patch(long id, ParentUpdateDto dto) {
        log.debug("Updating parent {} with {} children", id, dto.getChildren() == null ? 0 : dto.getChildren().size());
        
        find(id);
        Set<Student> children = resolveChildren(dto.getChildren());
        Parent parent = super.patch(id, dto);
        if (!children.isEmpty()) {
            parent.setChildren(children);
        }

        return parent;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Parent find(long id) {
        return parentRepository.findByIdAndSchoolId(id, currentSchoolResolver.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Parent> findAll(Pageable pageable) {
        return parentRepository.findAll(schoolMembershipSpecification(), pageable);
    }

    private Specification<Parent> schoolMembershipSpecification() {
        return PeopleDirectorySpecifications.guardiansInSchool(currentSchoolResolver.resolve().getId());
    }

    /* ---------- Enhanced filtering methods ---------- */
    
    @Transactional(readOnly = true)
    public Page<Parent> findAllWithFilters(Pageable pageable,
                                         String firstNameLike,
                                         String lastNameLike,
                                         String emailLike,
                                         String telephoneLike,
                                         String preferredContactMethodLike) {
        
        Specification<Parent> spec = schoolMembershipSpecification();
        
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
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap, FILTER_FIELDS);
        Specification<Parent> spec = schoolMembershipSpecification()
                .and(DynamicSpecificationBuilder.build(criteria));
        return parentRepository.findAll(spec, pageable);
    }

    /* ---------- Search method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Parent> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        Specification<Parent> spec = (root, q, cb) -> 
            cb.or(
                cb.like(cb.lower(root.get("firstName")), searchTerm),
                cb.like(cb.lower(root.get("lastName")), searchTerm),
                cb.like(cb.lower(root.get("email")), searchTerm),
                cb.like(cb.lower(root.get("telephone")), searchTerm)
            );
        
        return parentRepository.findAll(schoolMembershipSpecification().and(spec), pageable);
    }
    
    private Set<Student> resolveChildren(List<String> childrenEmails) {
        Set<Student> children = new HashSet<>();
        if (childrenEmails == null || childrenEmails.isEmpty()) {
            return children;
        }
        Long schoolId = currentSchoolResolver.resolve().getId();
        for (String email : childrenEmails) {
            children.add(studentRepository.findByEmailAndSchoolId(email, schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
        }
        return children;
    }
}
