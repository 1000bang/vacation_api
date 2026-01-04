package com.vacation.api.domain.vacation.request;

import com.vacation.api.enums.VacationType;
import com.vacation.api.validation.DateRangeValidator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 연차 신청 샘플 요청 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Data
@DateRangeValidator
public class VacationSampleRequest {

    /**
     * 신청일자
     */
    @NotNull(message = "신청일자는 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestDate;

    /**
     * 소속
     */
    @NotNull(message = "소속은 필수입니다.")
    private String department;

    /**
     * 신청자
     */
    @NotNull(message = "신청자는 필수입니다.")
    private String applicant;

    /**
     * 시작일
     */
    @NotNull(message = "시작일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 종료일
     */
    @NotNull(message = "종료일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 휴가 구분
     */
    @NotNull(message = "휴가 구분은 필수입니다.")
    private VacationType vacationType;

    /**
     * 사유
     */
    private String reason;

    /**
     * 총연차일수 (0.5 단위 가능)
     */
    @NotNull(message = "총 연차일수는 필수입니다.")
    private Double totalVacationDays;

    /**
     * 잔여연차일수 (0.5 단위 가능)
     */
    @NotNull(message = "잔여 연차일수는 필수입니다.")
    private Double remainingVacationDays;

    /**
     * 신청연차일수 (0.5 단위 가능)
     */
    @NotNull(message = "신청 연차일수는 필수입니다.")
    private Double requestedVacationDays;
}

