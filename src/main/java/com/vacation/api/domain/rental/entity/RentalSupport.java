package com.vacation.api.domain.rental.entity;

import com.vacation.api.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월세 지원 신청 정보 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Entity
@Table(name = "tbl_rental_support")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalSupport {

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
     * 신청일자
     */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /**
     * 청구 년월 (YYYYMM 형식, 예: 202501)
     */
    @Column(name = "billing_yy_month", nullable = false)
    private Integer billingYyMonth;

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
     * 선불/후불 구분
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    /**
     * 월세 청구 개시일
     */
    @Column(name = "billing_start_date", nullable = false)
    private LocalDate billingStartDate;

    /**
     * 청구 월세 시작일
     */
    @Column(name = "billing_period_start_date", nullable = false)
    private LocalDate billingPeriodStartDate;

    /**
     * 청구 월세 종료일
     */
    @Column(name = "billing_period_end_date", nullable = false)
    private LocalDate billingPeriodEndDate;

    /**
     * 월세 납입일
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * 월세 납입 금액 (원)
     */
    @Column(name = "payment_amount", nullable = false)
    private Long paymentAmount;

    /**
     * 월세 청구 금액 (원)
     */
    @Column(name = "billing_amount", nullable = false)
    private Long billingAmount;

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

