package com.vacation.api.util;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
            return "  " + formatted;  // 한 자리: 앞에 스페이스 두 개
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
}

