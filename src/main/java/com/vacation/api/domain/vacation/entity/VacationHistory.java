package com.vacation.api.domain.vacation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 연차 내역 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Entity
@Table(name = "tbl_vacation_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationHistory {

    /**
     * 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 사용자 ID (FK)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 시작일
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 종료일
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 신청연차일수
     */
    @Column(name = "period", nullable = false)
    private Double period;

    /**
     * 휴가 구분
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /**
     * 사유
     */
    @Column(name = "reason", length = 1000)
    private String reason;

    /**
     * 신청일자
     */
    @Column(name = "req_date", nullable = false)
    private LocalDate requestDate;

    /**
     * 금년 발생 연차
     */
    @Column(name = "annual_vacation_days", nullable = false)
    private Double annualVacationDays;

    /**
     * 직전 남은 연차 (신청 시점의 남은 연차)
     */
    @Column(name = "previous_remaining_days", nullable = false)
    private Double previousRemainingDays;

    /**
     * 사용 연차 (이번에 신청한 period)
     */
    @Column(name = "used_vacation_days", nullable = false)
    private Double usedVacationDays;

    /**
     * 남은 연차 (직전 남은 연차 - 이번에 신청한 period)
     */
    @Column(name = "remaining_vacation_days", nullable = false)
    private Double remainingVacationDays;

    /**
     * 상태 (R: 예약중, C: 사용 연차에 카운트됨)
     */
    @Column(name = "status", nullable = false, length = 1)
    private String status;

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

