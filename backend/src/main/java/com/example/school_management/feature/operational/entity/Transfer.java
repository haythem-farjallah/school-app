package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.operational.entity.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transfers")
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "transfer_status")
    @ColumnTransformer(write = "?::transfer_status")
    private TransferStatus status = TransferStatus.PENDING;

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime effectiveAt;
    private String reason;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
} 