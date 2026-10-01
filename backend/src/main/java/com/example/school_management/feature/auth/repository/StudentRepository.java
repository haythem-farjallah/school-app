package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends BaseUserRepository<Student>, JpaSpecificationExecutor<Student> {
    long countByStatus(Status status);
    
    // Find students by parent ID through the parent_students join table
    @Query("SELECT s FROM Student s, Parent p WHERE s MEMBER OF p.children AND p.id = :parentId")
    List<Student> findByParentId(@Param("parentId") Long parentId);
    
    // Find students by class IDs through enrollments
    @Query("""
        SELECT DISTINCT s FROM Student s 
        JOIN s.enrollments e 
        WHERE e.classEntity.id IN :classIds 
        AND e.status = 'ACTIVE'
    """)
    List<Student> findByClassIds(@Param("classIds") List<Long> classIds);

    // Students who can receive a new ACTIVE Enrollment in an AcademicYear: an ACTIVE account with an ACTIVE STUDENT
    // membership in the School, and no ACTIVE Enrollment yet in that AcademicYear.
    @Query("""
        SELECT s FROM Student s
        WHERE CAST(s.status AS string) = :userStatus
        AND EXISTS (
            SELECT 1 FROM SchoolMembership m
            WHERE m.user.id = s.id AND m.school.id = :schoolId
            AND m.status = :membershipStatus AND :role MEMBER OF m.roles)
        AND NOT EXISTS (
            SELECT 1 FROM Enrollment e
            WHERE e.student.id = s.id AND e.status = :enrollmentStatus
            AND e.classEntity.academicYear.id = :academicYearId)
    """)
    List<Student> findEnrollableStudents(@Param("schoolId") Long schoolId,
                                         @Param("academicYearId") Long academicYearId,
                                         @Param("userStatus") String userStatus,
                                         @Param("membershipStatus") MembershipStatus membershipStatus,
                                         @Param("role") MembershipRole role,
                                         @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus);

    default List<Student> findEnrollableStudents(Long schoolId, Long academicYearId) {
        return findEnrollableStudents(schoolId, academicYearId, Status.ACTIVE.name(), MembershipStatus.ACTIVE,
                MembershipRole.STUDENT, EnrollmentStatus.ACTIVE);
    }
}
