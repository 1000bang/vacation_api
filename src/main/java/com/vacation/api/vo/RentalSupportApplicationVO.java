package com.vacation.api.vo;

import com.vacation.api.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 월세지원 청구서 문서 생성용 VO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalSupportApplicationVO {

    private LocalDate requestDate;
    private Integer month;
    private String department;
    private String applicant;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Long contractMonthlyRent;
    private PaymentType paymentType;
    private LocalDate billingStartDate;
    private LocalDate billingPeriodStartDate;
    private LocalDate billingPeriodEndDate;
    private LocalDate paymentDate;
    private Long paymentAmount;
    private Long billingAmount;
}
