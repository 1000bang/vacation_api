package com.vacation.api.domain.vacation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 휴가 신청 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VacationRequest {

    /**
     * 신청일자
     */
    @NotNull(message = "신청일자는 필수입니다")
    private LocalDate requestDate;

    /**
     * 시작일
     */
    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    /**
     * 종료일
     */
    @NotNull(message = "종료일은 필수입니다")
    private LocalDate endDate;

    /**
     * 신청연차일수
     */
    @NotNull(message = "신청연차일수는 필수입니다")
    private Double period;

    /**
     * 휴가 구분
     */
    @NotBlank(message = "휴가 구분은 필수입니다")
    private String vacationType;

    /**
     * 사유
     */
    private String reason;
}

