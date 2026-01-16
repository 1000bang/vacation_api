package com.vacation.api.domain.vacation.response;

import com.vacation.api.domain.attachment.response.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 휴가 내역 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationHistoryResponse {
    private Long seq;
    private Long userId;
    private String applicant;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double period;
    private String type;
    private String reason;
    private LocalDate requestDate;
    private Double annualVacationDays;
    private Double previousRemainingDays;
    private Double usedVacationDays;
    private Double remainingVacationDays;
    private String status;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private AttachmentResponse attachment;
    private String rejectionReason;
}
