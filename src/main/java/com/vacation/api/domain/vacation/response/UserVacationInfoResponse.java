package com.vacation.api.domain.vacation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자별 연차 정보 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVacationInfoResponse {

    /**
     * 시퀀스
     */
    private Long seq;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 금년 발생 연차
     */
    private Double annualVacationDays;

    /**
     * 사용 연차
     */
    private Double usedVacationDays;

    /**
     * 예약중 연차
     */
    private Double reservedVacationDays;

    /**
     * 잔여 연차 (금년 발생 연차 - 사용 연차 - 예약중 연차)
     */
    private Double remainingVacationDays;


    /**
     * 최초 로그인 여부
     */
    private boolean isFirstLogin;
}

