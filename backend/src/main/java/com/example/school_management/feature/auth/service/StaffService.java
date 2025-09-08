package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.feature.auth.dto.StaffCreateDto;
import com.example.school_management.feature.auth.dto.StaffDto;
import com.example.school_management.feature.auth.dto.StaffUpdateDto;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.enums.StaffType;
import com.example.school_management.feature.auth.mapper.StaffMapper;
import com.example.school_management.feature.auth.repository.StaffRepository;
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

import java.util.List;

import java.util.Map;

@Service
@Slf4j
@Transactional
public class StaffService extends AbstractUserCrudService<
        Staff, StaffCreateDto, StaffUpdateDto, StaffDto> {

    private final StaffRepository staffRepo;

    public StaffService(StaffRepository repo,
                        StaffMapper mapper,
                        PasswordEncoder enc,
                        PasswordUtil pw,
                        ApplicationEventPublisher ev,
                        AuditService auditService,
                        UserRepository userRepository) {
        super(repo, mapper, enc, pw, ev, auditService, userRepository);
        this.staffRepo = repo;
    }

    /* ---------- Enhanced filtering methods ---------- */
    
    @Transactional(readOnly = true)
    public Page<Staff> findAllWithFilters(Pageable pageable,
                                        String search,
                                        String firstNameLike,
                                        String lastNameLike,
                                        String emailLike,
                                        String staffType,
                                        String department) {
        
        // If search is provided, use the search method
        if (search != null && !search.isBlank()) {
            return search(pageable, search);
        }
        
        Specification<Staff> spec = Specification.where(null);
        
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
        
        if (staffType != null && !staffType.isBlank()) {
            try {
                StaffType type = StaffType.valueOf(staffType.toUpperCase());
                spec = spec.and((root, query, cb) -> 
                    cb.and(
                        cb.isNotNull(root.get("staffType")),
                        cb.equal(root.get("staffType"), type)
                    ));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid staff type: {}", staffType);
            }
        }
        
        if (department != null && !department.isBlank()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("department")), "%" + department.toLowerCase() + "%"));
        }
        
        return staffRepo.findAll(spec, pageable);
    }

    /* ---------- Advanced filtering method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Staff> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<Staff> spec = DynamicSpecificationBuilder.build(criteria);
        return staffRepo.findAll(spec, pageable);
    }

    /* ---------- Search method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Staff> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return staffRepo.findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        Specification<Staff> spec = (root, q, cb) -> 
            cb.or(
                cb.like(cb.lower(root.get("firstName")), searchTerm),
                cb.like(cb.lower(root.get("lastName")), searchTerm),
                cb.like(cb.lower(root.get("email")), searchTerm),
                cb.like(cb.lower(root.get("department")), searchTerm)
            );
        
        return staffRepo.findAll(spec, pageable);
    }

    /* ---------- Bulk operations ---------- */
    
    @Transactional
    public void bulkDelete(List<Long> ids) {
        log.debug("Bulk deleting {} staff members", ids.size());
        for (Long id : ids) {
            delete(id); // Uses the existing soft delete method
        }
        log.info("Bulk deleted {} staff members", ids.size());
    }
    
    @Transactional
    public void bulkUpdateStatus(List<Long> ids, String status) {
        log.debug("Bulk updating status for {} staff members to {}", ids.size(), status);
        List<Staff> staff = staffRepo.findAllById(ids);
        for (Staff member : staff) {
            member.setStatus(com.example.school_management.feature.auth.entity.Status.valueOf(status.toUpperCase()));
        }
        staffRepo.saveAll(staff);
        log.info("Bulk updated status for {} staff members", staff.size());
    }
    
    @Transactional(readOnly = true)
    public List<Staff> findByIds(List<Long> ids) {
        return staffRepo.findAllById(ids);
    }
    
    @Transactional(readOnly = true)
    public List<Staff> findAll() {
        return staffRepo.findAll();
    }
} 