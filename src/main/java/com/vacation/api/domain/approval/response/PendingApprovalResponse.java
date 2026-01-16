package com.vacation.api.domain.approval.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 승인 대기 목록 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApprovalResponse {

    private ApplicationList vacation;
    private ApplicationList expense;
    private ApplicationList rental;
    private ApplicationList rentalProposal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationList {
        private List<ApplicationItem> list;
        private Long totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationItem {
        private String applicationType;
        private Long seq;
        private Long userId;
        private String applicant;
        private String approvalStatus;
        private LocalDateTime createdAt;

        // VACATION 필드
        private LocalDate startDate;
        private LocalDate endDate;
        private Double period;
        private String type;
        private String reason;

        // EXPENSE 필드
        private LocalDate requestDate;
        private Integer billingYyMonth;
        private Integer childCnt;
        private Long totalAmount;

        // RENTAL 필드
        private Long contractMonthlyRent;
        private Long billingAmount;
        private LocalDate paymentDate;
        private LocalDate requestDateRental; // RENTAL의 requestDate
        private Integer billingYyMonthRental; // RENTAL의 billingYyMonth

        // RENTAL_PROPOSAL 필드
        private String rentalAddress;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private LocalDate billingStartDate;
        private Long contractMonthlyRentProposal; // RENTAL_PROPOSAL의 contractMonthlyRent
        private Long billingAmountProposal; // RENTAL_PROPOSAL의 billingAmount
    }
}
