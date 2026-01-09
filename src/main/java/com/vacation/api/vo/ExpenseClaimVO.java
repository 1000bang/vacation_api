package com.vacation.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 개인 비용 청구서 문서 생성용 VO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimVO {

    private LocalDate requestDate;
    private Integer month;
    private String department;
    private String applicant;
    private List<ExpenseItemVO> expenseItems;

    /**
     * 비용 항목 VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItemVO {
        private LocalDate date;
        private String usageDetail;
        private String vendor;
        private String paymentMethod;
        private String project;
        private Long amount;
        private String note;
    }
}
