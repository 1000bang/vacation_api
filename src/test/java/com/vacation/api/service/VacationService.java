package com.vacation.api.service;

import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 서비스 로직 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
public class VacationService {

    /**
     * 계약 기간을 "N년 M개월" 형식으로 계산하는 메서드
     *
     * @param startDate 계약 시작일
     * @param endDate 계약 종료일
     * @return "1년", "2년", "2년 6개월" 형식의 문자열
     */
    public static String calculateContractPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "";
        }

        // 전체 개월 수 계산
        long totalMonths = ChronoUnit.MONTHS.between(startDate, endDate);
        
        // 년 수 계산
        long years = totalMonths / 12;
        // 남은 개월 수 계산
        long months = totalMonths % 12;

        // 결과 문자열 생성
        if (years == 0 && months == 0) {
            // 시작일과 종료일이 같은 경우
            return "0개월";
        } else if (years == 0) {
            // 1년 미만인 경우
            return months + "개월";
        } else if (months == 0) {
            // 정확히 N년인 경우
            return years + "년";
        } else {
            // N년 M개월인 경우
            return years + "년 " + months + "개월";
        }
    }

    @Test
    @DisplayName("계약 기간 계산 - 11개월")
    void testCalculateContractPeriod_1Year() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 20);

        // when
        String result = calculateContractPeriod(startDate, endDate);

        // then
        assertEquals("11개월", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - Request 사용 11개월")
    void testCalculateContractPeriod_1Year_WithRequest() {
        // given
        RentalSupportSampleRequest request = new RentalSupportSampleRequest();
        request.setContractStartDate(LocalDate.of(2025, 1, 1));
        request.setContractEndDate(LocalDate.of(2025, 12, 20));

        // when
        String result = calculateContractPeriod(
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        // then
        assertEquals("11개월", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - 정확히 1년")
    void testCalculateContractPeriod_Exactly1Year() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 1);

        // when
        String result = calculateContractPeriod(startDate, endDate);

        // then
        assertEquals("1년", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - Request 사용 정확히 1년")
    void testCalculateContractPeriod_Exactly1Year_WithRequest() {
        // given
        RentalSupportSampleRequest request = new RentalSupportSampleRequest();
        request.setContractStartDate(LocalDate.of(2025, 1, 1));
        request.setContractEndDate(LocalDate.of(2026, 1, 1));

        // when
        String result = calculateContractPeriod(
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        // then
        assertEquals("1년", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - 2년 6개월")
    void testCalculateContractPeriod_2Years6Months() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2027, 7, 1);

        // when
        String result = calculateContractPeriod(startDate, endDate);

        // then
        assertEquals("2년 6개월", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - Request 사용 정확히 2년 6개월")
    void testCalculateContractPeriod_2Years6Months_WithRequest() {
        // given
        RentalSupportSampleRequest request = new RentalSupportSampleRequest();
        request.setContractStartDate(LocalDate.of(2025, 1, 1));
        request.setContractEndDate(LocalDate.of(2027, 7, 20));

        // when
        String result = calculateContractPeriod(
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        // then
        assertEquals("2년 6개월", result);
    }

    @Test
    @DisplayName("계약 기간 계산 - null 처리")
    void testCalculateContractPeriod_NullHandling() {
        // given
        LocalDate startDate = null;
        LocalDate endDate = LocalDate.of(2025, 1, 1);

        // when
        String result = calculateContractPeriod(startDate, endDate);

        // then
        assertEquals("", result);
    }

    /**
     * trouble
     * ChronoUnit.YEARS.between()을 사용한 계약 기간 계산 테스트
     * 계약기간이 2년 1년으로 나옴
     */
    @Test
    @DisplayName("ChronoUnit.YEARS.between - 1년")
    void testContractYears_1Year() {
        // given
        RentalSupportSampleRequest request = new RentalSupportSampleRequest();
        request.setContractStartDate(LocalDate.of(2025, 1, 1));
        request.setContractEndDate(LocalDate.of(2026, 12, 31));

        // when
        long contractYears = ChronoUnit.YEARS.between(
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        // then
        assertEquals(1, contractYears);
    }

    @Test
    @DisplayName("NewLogic - 1년 11개월")
    void testContractYears_1Year_V2() {
        // given
        RentalSupportSampleRequest request = new RentalSupportSampleRequest();
        request.setContractStartDate(LocalDate.of(2025, 1, 1));
        request.setContractEndDate(LocalDate.of(2026, 12, 31));

        // when
        String result = calculateContractPeriod(
                request.getContractStartDate(),
                request.getContractEndDate()
        );

        // then
        assertEquals("1년 11개월", result);
    }

}
