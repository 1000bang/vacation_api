package com.vacation.api.domain.expense.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 비용 항목 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseItemRequest {

    /**
     * 일자
     */
    @NotNull(message = "일자는 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
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
    @NotNull(message = "금액은 필수입니다")
    private Long amount;

    /**
     * 비고
     */
    private String note;
}

