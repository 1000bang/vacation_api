package com.vacation.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 월세지원 품의서 문서 생성용 VO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalSupportProposalVO {

    private LocalDate requestDate;
    private String department;
    private String applicant;
    private String currentAddress;
    private String rentalAddress;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Long contractMonthlyRent;
    private Long billingAmount;
    private LocalDate billingStartDate;
    private String reason;
}
