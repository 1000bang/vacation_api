package com.vacation.api.vo;

import com.vacation.api.enums.VacationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 연차 신청서 문서 생성용 VO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationDocumentVO {

    private LocalDate requestDate;
    private String department;
    private String applicant;
    private LocalDate startDate;
    private LocalDate endDate;
    private VacationType vacationType;
    private String reason;
    private Double totalVacationDays;
    private Double remainingVacationDays;
    private Double requestedVacationDays;
    private Double usedVacationDays; // 사용 연차
}
