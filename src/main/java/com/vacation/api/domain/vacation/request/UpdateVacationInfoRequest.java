package com.vacation.api.domain.vacation.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 연차 정보 수정 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVacationInfoRequest {

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
}

