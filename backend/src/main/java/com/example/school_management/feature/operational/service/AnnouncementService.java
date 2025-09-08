package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    /* ─── CRUD ─────────────────────────────────────────── */
    AnnouncementDto create(CreateAnnouncementRequest req);
    AnnouncementDto update(Long id, UpdateAnnouncementRequest req);
    void delete(Long id);
    AnnouncementDto get(Long id);

    /* ─── LIST with filters ─────────────────────────────── */
    Page<AnnouncementDto> list(Pageable page, String importance, Boolean isPublic);
    Page<AnnouncementDto> getPublicAnnouncements(Pageable page);

    /* ─── PUBLISHING ────────────────────────────────────── */
    AnnouncementDto publish(Long id, PublishAnnouncementRequest req);
    
    /* ─── HELPER METHODS ─────────────────────────────────── */
    Object getTeacherClasses();
    void createTestTeachingAssignments();
} 