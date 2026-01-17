package com.vacation.api.domain.alarm.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알람 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmResponse {
    private Long seq;
    private Long userId;
    private String alarmType; // A, B, C, RB, RC (ApprovalStatus enum의 name)
    private String applicationType; // VACATION, EXPENSE, RENTAL, RENTAL_PROPOSAL
    private Long applicationSeq;
    private String message;
    private Boolean isRead;
    private String redirectUrl; // 실제 URL (/my-applications, /approval-list)
    private LocalDateTime createdAt;
}
