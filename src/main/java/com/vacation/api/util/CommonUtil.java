package com.vacation.api.util;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 공통 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
public class CommonUtil {

    /**
     * 문서 번호 생성
     */
    public static String generateDocumentNumber(LocalDate requestDate) {
        try {
            String dateStr = requestDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return "KP-" + dateStr + "-01";
        } catch (Exception e) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return "KP-" + dateStr + "-01";
        }
    }

    /**
     * 날짜 포맷팅 (yyyy-MM-dd -> yyyy년 MM월 dd일)
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
    }

    /**
     * 날짜 포맷팅 (yyyy-MM-dd -> yyyy.MM.dd)
     */
    public static String formatDateShort(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    /**
     * 숫자 포맷팅 (천 단위 구분 기호)
     */
    public static String formatNumber(Long number) {
        if (number == null) {
            return "0";
        }
        DecimalFormat formatter = new DecimalFormat("###,###");
        return formatter.format(number);
    }

    /**
     * 기간 포맷팅 (시작일 ~ 종료일)
     */
    public static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "";
        }
        return formatDate(startDate) + " ~ " + formatDate(endDate);
    }

    /**
     * 연차일수 포맷팅 (정수면 정수로, 소수면 소수로)
     * 자간을 맞추기 위해 세 자리면 그대로, 두 자리면 앞에 스페이스 하나, 한 자리면 앞에 스페이스 두 개 추가
     */
    public static String formatVacationDays(Double days) {
        if (days == null) {
            return "  0";
        }

        String formatted;
        if (days % 1 == 0) {
            formatted = String.valueOf(days.intValue());
        } else {
            formatted = String.format("%.1f", days);
        }

        // 자간 맞추기: 세 자리면 그대로, 두 자리면 앞에 스페이스 하나, 한 자리면 앞에 스페이스 두 개
        int length = formatted.length();
        if (length == 1) {
            return "  " + formatted ;  // 한 자리: 앞에 스페이스 두 개
        } else if (length == 2) {
            return " " + formatted;   // 두 자리: 앞에 스페이스 하나
        } else {
            return formatted;         // 세 자리 이상: 그대로
        }
    }

    /**
     * 최종 잔여 연차일수 계산
     */
    public static Double calculateFinalRemainingDays(Double previousRemainingDays, Double requestedDays) {
        if (previousRemainingDays == null || requestedDays == null) {
            return null;
        }
        return previousRemainingDays - requestedDays;
    }

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

    /**
     * 계약 기간의 년 수 계산 (ChronoUnit.YEARS.between과 동일)
     *
     * @param startDate 계약 시작일
     * @param endDate 계약 종료일
     * @return 완전한 년 수
     */
    public static long calculateContractYears(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(startDate, endDate);
    }
}

