package com.vacation.api.util;

import java.time.LocalDate;

/**
 * 청구 관련 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
public class BillingUtil {

    /**
     * 청구 년월 계산 (YYYYMM 형식)
     * requestDate가 1월이고 month가 12이면 전년도 12월로 설정
     *
     * @param requestDate 신청일자
     * @param month 청구 월 (1-12)
     * @return 청구 년월 (YYYYMM 형식, 예: 202601)
     */
    public static int calculateBillingYyMonth(LocalDate requestDate, int month) {
        int requestYear = requestDate.getYear();
        int requestMonth = requestDate.getMonthValue();
        int year = requestYear;
        
        // requestDate가 1월이고 month가 12이면 전년도 사용
        if (requestMonth == 1 && month == 12) {
            year = requestYear - 1;
        }
        
        return year * 100 + month;
    }
}
