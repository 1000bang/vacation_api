package com.vacation.api.domain.sample.request;

import com.vacation.api.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 월세지원 청구 요청 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Data
public class RentalSupportSampleRequest {

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
     * 월세 총 계약 시작일
     */
    @NotNull(message = "월세 총 계약 시작일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractStartDate;

    /**
     * 월세 총 계약 종료일
     */
    @NotNull(message = "월세 총 계약 종료일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractEndDate;

    /**
     * 계약 월세 금액(원)
     */
    @NotNull(message = "계약 월세 금액은 필수입니다.")
    private Long contractMonthlyRent;

    /**
     * 선불/후불 구분
     */
    @NotNull(message = "선불/후불 구분은 필수입니다.")
    private PaymentType paymentType;

    /**
     * 월세 청구 개시일
     */
    @NotNull(message = "월세 청구 개시일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billingStartDate;

    /**
     * 청구 월세 시작일
     */
    @NotNull(message = "청구 월세 시작일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billingPeriodStartDate;

    /**
     * 청구 월세 종료일
     */
    @NotNull(message = "청구 월세 종료일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billingPeriodEndDate;

    /**
     * 월세 납입일
     */
    @NotNull(message = "월세 납입일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;

    /**
     * 월세 납입 금액(원)
     */
    @NotNull(message = "월세 납입 금액은 필수입니다.")
    private Long paymentAmount;

    /**
     * 월세 청구 금액(원)
     */
    @NotNull(message = "월세 청구 금액은 필수입니다.")
    private Long billingAmount;
}

