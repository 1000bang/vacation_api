package com.vacation.api.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 개인 비용 청구 정보 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Entity
@Table(name = "tbl_expense_claim")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaim {

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
     * 자식 항목 개수
     */
    @Column(name = "child_cnt", nullable = false)
    private Integer childCnt;

    /**
     * 총 금액
     */
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

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

