package com.vacation.api.domain.rental.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 월세 품의 신청 정보 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalProposalRequest {

    /**
     * 기존 거주지 주소
     */
    private String previousAddress;

    /**
     * 월세 계약 주소
     */
    @NotBlank(message = "월세 계약 주소는 필수입니다")
    private String rentalAddress;

    /**
     * 월세 계약 시작일
     */
    @NotNull(message = "월세 계약 시작일은 필수입니다")
    private LocalDate contractStartDate;

    /**
     * 월세 계약 종료일
     */
    @NotNull(message = "월세 계약 종료일은 필수입니다")
    private LocalDate contractEndDate;

    /**
     * 계약 월세 금액 (원)
     */
    @NotNull(message = "계약 월세 금액은 필수입니다")
    private Long contractMonthlyRent;

    /**
     * 월세 청구 금액 (원)
     */
    @NotNull(message = "월세 청구 금액은 필수입니다")
    private Long billingAmount;

    /**
     * 월세 청구 개시일
     */
    @NotNull(message = "월세 청구 개시일은 필수입니다")
    private LocalDate billingStartDate;

    /**
     * 월세 청구 사유
     */
    private String billingReason;
}
