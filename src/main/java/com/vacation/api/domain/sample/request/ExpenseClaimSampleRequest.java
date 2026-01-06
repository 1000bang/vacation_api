package com.vacation.api.domain.sample.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 업무관련 개인 비용 청구서 요청 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Data
public class ExpenseClaimSampleRequest {

    /**
     * 신청일자
     */
    @NotNull(message = "신청일자는 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestDate;

    /**
     * 청구 월
     */
    @NotNull(message = "청구 월은 필수입니다.")
    private Integer month;

    /**
     * 소속
     */
    @NotNull(message = "소속은 필수입니다.")
    private String department;

    /**
     * 신청자
     */
    @NotNull(message = "신청자는 필수입니다.")
    private String applicant;

    /**
     * 비용 항목 목록
     */
    @NotNull(message = "비용 항목 목록은 필수입니다.")
    private List<ExpenseItem> expenseItems;

}

