package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {
    Optional<Announcement> findByIdAndSchoolId(Long id, Long schoolId);
} 