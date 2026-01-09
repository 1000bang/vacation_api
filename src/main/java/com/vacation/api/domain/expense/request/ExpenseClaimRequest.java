package com.vacation.api.domain.expense.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 개인 비용 청구 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimRequest {

    /**
     * 신청일자
     */
    @NotNull(message = "신청일자는 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestDate;

    /**
     * 청구 월
     */
    @NotNull(message = "청구 월은 필수입니다")
    private Integer month;

    /**
     * 비용 항목 목록
     */
    @NotEmpty(message = "비용 항목 목록은 필수입니다")
    private List<ExpenseItemRequest> expenseItems;
}

