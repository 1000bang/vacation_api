package com.vacation.api.domain.expense.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개인 비용 청구 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimResponse {
    private Long seq;
    private Long userId;
    private String applicant;
    private LocalDate requestDate;
    private Integer billingYyMonth;
    private Integer childCnt;
    private Long totalAmount;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private List<ExpenseSubResponse> expenseSubList;
}
