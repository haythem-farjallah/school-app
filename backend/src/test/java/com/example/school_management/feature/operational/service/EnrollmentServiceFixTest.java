/*package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.impl.EnrollmentServiceImpl;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.operational.service.impl.RealTimeNotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceFixTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;
    
    @Mock
    private StudentRepository studentRepo;
    
    @Mock
    private ClassRepository classRepo;
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private BaseUserRepository<BaseUser> userRepo;
    
    @Mock
    private RealTimeNotificationService realTimeNotificationService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Test
    void testRepositoryMethodsUseCorrectEnumParameters() {
        // This test verifies that the repository methods are called with proper enum parameters
        // instead of string literals, which was causing the PostgreSQL enum comparison error
        
        Long studentId = 1L;
        Long classId = 1L;
        
        // Test canEnrollStudent method which internally calls countActiveEnrollmentsByClassId
        when(enrollmentRepo.findByStudentIdAndClassId(studentId, classId))
            .thenReturn(java.util.Optional.empty());
        when(enrollmentRepo.countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE))
            .thenReturn(25L);
        
        // When
        boolean canEnroll = enrollmentService.canEnrollStudent(studentId, classId);
        
        // Then
        assertTrue(canEnroll);
        // Verify that the method is called with the enum parameter, not a string literal
        verify(enrollmentRepo).countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE);
    }

    @Test
    void testCanEnrollStudent_WithAvailableCapacity_ReturnsTrue() {
        // Given
        Long studentId = 1L;
        Long classId = 1L;
        
        when(enrollmentRepo.findByStudentIdAndClassId(studentId, classId))
            .thenReturn(java.util.Optional.empty());
        when(enrollmentRepo.countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE))
            .thenReturn(25L);
        
        // When
        boolean canEnroll = enrollmentService.canEnrollStudent(studentId, classId);
        
        // Then
        assertTrue(canEnroll);
        verify(enrollmentRepo).countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE);
    }

    @Test
    void testCanEnrollStudent_WithFullCapacity_ReturnsFalse() {
        // Given
        Long studentId = 1L;
        Long classId = 1L;
        
        when(enrollmentRepo.findByStudentIdAndClassId(studentId, classId))
            .thenReturn(java.util.Optional.empty());
        when(enrollmentRepo.countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE))
            .thenReturn(30L);
        
        // When
        boolean canEnroll = enrollmentService.canEnrollStudent(studentId, classId);
        
        // Then
        assertFalse(canEnroll);
        verify(enrollmentRepo).countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE);
    }
}
*/