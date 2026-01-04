package com.vacation.api.domain.vacation.request;

import lombok.Data;

/**
 * 연차 신청 요청 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Data
public class VacationRequestData {

    private String userId;
    private String startDate;
    private String endDate;
    private String reason;
    // TODO: 추가 필드 정의 예정
}
