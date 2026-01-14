package com.vacation.api.domain.rental.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월세 품의 신청 정보 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Entity
@Table(name = "tbl_rental_approval")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalApproval {

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
     * 기존 거주지 주소
     */
    @Column(name = "previous_address", length = 500)
    private String previousAddress;

    /**
     * 월세 계약 주소
     */
    @Column(name = "rental_address", length = 500, nullable = false)
    private String rentalAddress;

    /**
     * 월세 계약 시작일
     */
    @Column(name = "contract_start_date", nullable = false)
    private LocalDate contractStartDate;

    /**
     * 월세 계약 종료일
     */
    @Column(name = "contract_end_date", nullable = false)
    private LocalDate contractEndDate;

    /**
     * 계약 월세 금액 (원)
     */
    @Column(name = "contract_monthly_rent", nullable = false)
    private Long contractMonthlyRent;

    /**
     * 월세 청구 금액 (원)
     */
    @Column(name = "billing_amount", nullable = false)
    private Long billingAmount;

    /**
     * 월세 청구 개시일
     */
    @Column(name = "billing_start_date", nullable = false)
    private LocalDate billingStartDate;

    /**
     * 월세 청구 사유
     */
    @Column(name = "billing_reason", length = 1000)
    private String billingReason;

    /**
     * 승인 상태 (A: 초기 생성, AM: 수정됨, B: 팀장 승인, RB: 팀장 반려, C: 본부장 승인, RC: 본부장 반려)
     */
    @Column(name = "approval_status", length = 2)
    @Builder.Default
    private String approvalStatus = "A";

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

