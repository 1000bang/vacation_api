package com.vacation.api.domain.vacation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 연차 정보 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Entity
@Table(name = "tbl_user_vacation_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVacationInfo {

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
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 금년 발생 연차
     */
    @Column(name = "annual_vacation_days", nullable = false)
    @Builder.Default
    private Double annualVacationDays = 0.0;

    /**
     * 사용 연차
     */
    @Column(name = "used_vacation_days", nullable = false)
    @Builder.Default
    private Double usedVacationDays = 0.0;

    /**
     * 예약중 연차 (신청은 했지만 아직 사용일에 도래하지 않음 - 수정 가능성을 위해)
     */
    @Column(name = "reserved_vacation_days", nullable = false)
    @Builder.Default
    private Double reservedVacationDays = 0.0;

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

