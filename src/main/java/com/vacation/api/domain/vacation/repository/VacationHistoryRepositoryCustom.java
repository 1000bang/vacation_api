package com.vacation.api.domain.vacation.repository;

import com.vacation.api.domain.vacation.entity.VacationHistory;

import java.time.LocalDate;
import java.util.List;

/**
 * VacationHistoryRepository 커스텀 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 정의
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-09
 */
public interface VacationHistoryRepositoryCustom {
    
    /**
     * 본부 사용자들의 날짜 범위 내 휴가 조회
     *
     * @param userIds 사용자 ID 목록
     * @param startDate 조회 시작일 (이 날짜 이후)
     * @param endDate 조회 종료일 (이 날짜 이전)
     * @return 휴가 내역 목록
     */
    List<VacationHistory> findByUserIdsAndDateRange(List<Long> userIds, LocalDate startDate, LocalDate endDate);
    
    /**
     * 본부와 날짜 범위로 휴가 조회 (User와 VacationHistory 조인)
     *
     * @param division 본부
     * @param authVals 권한 값 목록
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 휴가 내역 목록
     */
    List<VacationHistory> findByDivisionAndDateRange(String division, List<String> authVals, LocalDate startDate, LocalDate endDate);
}
