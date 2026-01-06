package com.vacation.api.domain.sample.request;

import com.vacation.api.validation.BillingAmountValidator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 월세지원 품의서 요청 데이터
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Data
@BillingAmountValidator
public class RentalSupportPropSampleRequest {

    /**
     * 신청일자 (품의 일자)
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
     * 신청자 (기안자)
     */
    @NotNull(message = "신청자는 필수입니다.")
    private String applicant;

    /**
     * 기존 거주지 주소
     */
    private String currentAddress;

    /**
     * 월세 계약 주소
     */
    private String rentalAddress;

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
     * 월세 청구 금액(원)
     */
    @NotNull(message = "월세 청구 금액은 필수입니다.")
    private Long billingAmount;

    /**
     * 월세 청구 개시일
     */
    @NotNull(message = "월세 청구 개시일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billingStartDate;

    /**
     * 월세 청구 사유
     */
    private String reason;
}

