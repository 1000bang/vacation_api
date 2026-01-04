package com.vacation.api.vo;

import lombok.Data;

/**
 * 연차 정보를 담는 Value Object
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Data
public class VacationVO {

    private Long vacationId;
    private String userId;
    private String startDate;
    private String endDate;
    private String reason;
    private String status;
    // TODO: 추가 필드 정의 예정
}

