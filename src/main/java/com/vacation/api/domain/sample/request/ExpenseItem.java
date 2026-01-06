package com.vacation.api.domain.sample.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 비용 항목 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Data
public class ExpenseItem {
    /**
     * 일자
     */
    private LocalDate date;

    /**
     * 사용 내역
     */
    private String usageDetail;

    /**
     * 거래처
     */
    private String vendor;

    /**
     * 결재방법
     */
    private String paymentMethod;

    /**
     * 프로젝트
     */
    private String project;

    /**
     * 금액(원)
     */
    private Long amount;

    /**
     * 비고
     */
    private String note;
}

