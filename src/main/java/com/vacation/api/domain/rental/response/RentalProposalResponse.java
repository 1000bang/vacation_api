package com.vacation.api.domain.rental.response;

import com.vacation.api.domain.attachment.response.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월세 품의 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalProposalResponse {
    private Long seq;
    private Long userId;
    private String applicant;
    private String previousAddress;
    private String rentalAddress;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Long contractMonthlyRent;
    private Long billingAmount;
    private LocalDate billingStartDate;
    private String billingReason;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private AttachmentResponse attachment;
}
