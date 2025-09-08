package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentStatsDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.mapper.StudentMapper;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class StudentService extends AbstractUserCrudService<
        Student, StudentCreateDto, StudentUpdateDto, StudentDto> {

    private final StudentRepository studentRepo;

    public StudentService(StudentRepository repo,
                         StudentMapper mapper,
                         PasswordEncoder enc,
                         PasswordUtil pw,
                         ApplicationEventPublisher ev,
                         AuditService auditService,
                         UserRepository userRepository) {
        super(repo, mapper, enc, pw, ev, auditService, userRepository);
        this.studentRepo = repo;
    }

    /* ---------- Enhanced filtering methods ---------- */
    
    @Transactional(readOnly = true)
    public Page<Student> findAllWithFilters(Pageable pageable,
                                          String firstNameLike,
                                          String lastNameLike,
                                          String emailLike,
                                          String gradeLevel,
                                          Integer enrollmentYear,
                                          String status) {
        
        Specification<Student> spec = Specification.where(null);
        
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
        
        if (gradeLevel != null && !gradeLevel.isBlank()) {
            try {
                GradeLevel level = GradeLevel.valueOf(gradeLevel.toUpperCase());
                spec = spec.and((root, query, cb) -> 
                    cb.and(
                        cb.isNotNull(root.get("gradeLevel")),
                        cb.equal(root.get("gradeLevel"), level)
                    ));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid grade level: {}", gradeLevel);
            }
        }
        
        if (enrollmentYear != null) {
            spec = spec.and((root, query, cb) -> 
                cb.and(
                    cb.isNotNull(root.get("enrolledAt")),
                    cb.equal(cb.function("YEAR", Integer.class, root.get("enrolledAt")), enrollmentYear)
                ));
        }
        
        if (status != null && !status.isBlank()) {
            try {
                Status studentStatus = Status.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), studentStatus));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
        
        return studentRepo.findAll(spec, pageable);
    }

    /* ---------- Advanced filtering method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Student> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<Student> spec = DynamicSpecificationBuilder.build(criteria);
        return studentRepo.findAll(spec, pageable);
    }

    /* ---------- Search method ---------- */
    
    @Transactional(readOnly = true)
    public Page<Student> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return studentRepo.findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        Specification<Student> spec = (root, q, cb) -> 
            cb.or(
                cb.like(cb.lower(root.get("firstName")), searchTerm),
                cb.like(cb.lower(root.get("lastName")), searchTerm),
                cb.like(cb.lower(root.get("email")), searchTerm)
            );
        
        return studentRepo.findAll(spec, pageable);
    }

    /* ---------- Bulk operations ---------- */
    
    public void bulkDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        log.info("Bulk deleting {} students", ids.size());
        List<Student> students = studentRepo.findAllById(ids);
        
        for (Student student : students) {
            student.setStatus(Status.DELETED);
        }
        
        studentRepo.saveAll(students);
        log.info("Bulk deleted {} students", students.size());
    }
    
    @Transactional
    public void bulkUpdateStatus(List<Long> ids, String status) {
        log.debug("Bulk updating status for {} students to {}", ids.size(), status);
        List<Student> students = studentRepo.findAllById(ids);
        for (Student student : students) {
            student.setStatus(Status.valueOf(status.toUpperCase()));
        }
        studentRepo.saveAll(students);
        log.info("Bulk updated status for {} students", students.size());
    }
    
    @Transactional(readOnly = true)
    public List<Student> findByIds(List<Long> ids) {
        return studentRepo.findAllById(ids);
    }
    
    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepo.findAll();
    }

    /* ---------- Statistics ---------- */
    
    @Transactional(readOnly = true)
    public StudentStatsDto getStats() {
        long totalStudents = studentRepo.count();
        long activeStudents = studentRepo.countByStatus(Status.ACTIVE);
        long suspendedStudents = studentRepo.countByStatus(Status.SUSPENDED);
        
        // Get students by grade level
        Map<String, Long> studentsByGradeLevel = studentRepo.findAll().stream()
                .filter(student -> student.getGradeLevel() != null)
                .collect(Collectors.groupingBy(
                    student -> student.getGradeLevel().name(),
                    Collectors.counting()
                ));
        
        // Get students by enrollment year
        Map<Integer, Long> studentsByEnrollmentYear = studentRepo.findAll().stream()
                .filter(student -> student.getEnrolledAt() != null)
                .collect(Collectors.groupingBy(
                    student -> student.getEnrolledAt().getYear(),
                    Collectors.counting()
                ));
        
        return new StudentStatsDto(
            totalStudents,
            activeStudents,
            suspendedStudents,
            studentsByGradeLevel,
            studentsByEnrollmentYear
        );
    }

}
