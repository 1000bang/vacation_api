package com.vacation.api.domain.rental.response;

import com.vacation.api.domain.attachment.response.AttachmentResponse;
import com.vacation.api.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월세 지원 신청 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalSupportResponse {
    private Long seq;
    private Long userId;
    private String applicant;
    private LocalDate requestDate;
    private Integer billingYyMonth;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Long contractMonthlyRent;
    private PaymentType paymentType;
    private LocalDate billingStartDate;
    private LocalDate billingPeriodStartDate;
    private LocalDate billingPeriodEndDate;
    private LocalDate paymentDate;
    private Long paymentAmount;
    private Long billingAmount;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private AttachmentResponse attachment;
}
