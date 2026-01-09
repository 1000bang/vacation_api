package com.vacation.api.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 개인 비용 청구 상세 항목 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Entity
@Table(name = "tbl_expense_sub")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSub {

    /**
     * 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 부모 시퀀스 (FK)
     */
    @Column(name = "parent_seq", nullable = false)
    private Long parentSeq;

    /**
     * 자식 번호
     */
    @Column(name = "child_no", nullable = false)
    private Integer childNo;

    /**
     * 일자
     */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /**
     * 사용 내역
     */
    @Column(name = "usage_detail", length = 500)
    private String usageDetail;

    /**
     * 거래처
     */
    @Column(name = "vendor", length = 500)
    private String vendor;

    /**
     * 결재방법
     */
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    /**
     * 프로젝트
     */
    @Column(name = "project", length = 200)
    private String project;

    /**
     * 금액(원)
     */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /**
     * 비고
     */
    @Column(name = "note", length = 1000)
    private String note;

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

