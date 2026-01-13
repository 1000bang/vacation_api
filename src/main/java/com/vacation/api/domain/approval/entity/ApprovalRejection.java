package com.vacation.api.domain.approval.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 승인 반려 사유 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-10
 */
@Entity
@Table(name = "tbl_approval_rejection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRejection {

    /**
     * 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 신청 타입 (VACATION, EXPENSE, RENTAL)
     */
    @Column(name = "application_type", nullable = false, length = 20)
    private String applicationType;

    /**
     * 신청 시퀀스 (vacation_seq, expense_seq, rental_seq)
     */
    @Column(name = "application_seq", nullable = false)
    private Long applicationSeq;

    /**
     * 반려한 사용자 ID
     */
    @Column(name = "rejected_by", nullable = false)
    private Long rejectedBy;

    /**
     * 반려 단계 (TEAM_LEADER: 팀장 반려, DIVISION_HEAD: 본부장 반려)
     */
    @Column(name = "rejection_level", nullable = false, length = 20)
    private String rejectionLevel;

    /**
     * 반려 사유
     */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /**
     * 생성일
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 엔티티 저장 전 실행 (생성일 설정)
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
