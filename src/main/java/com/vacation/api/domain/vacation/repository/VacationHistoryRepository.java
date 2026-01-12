package com.vacation.api.domain.vacation.repository;

import com.vacation.api.domain.vacation.entity.VacationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 연차 내역 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Repository
public interface VacationHistoryRepository extends JpaRepository<VacationHistory, Long>, VacationHistoryRepositoryCustom {

    /**
     * 사용자 ID로 연차 내역 목록 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByUserIdOrderBySeqDesc(Long userId);

    /**
     * 사용자 ID와 시퀀스로 연차 내역 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 연차 내역
     */
    java.util.Optional<VacationHistory> findBySeqAndUserId(Long seq, Long userId);

    /**
     * 종료일이 특정 날짜인 연차 내역 조회
     *
     * @param endDate 종료일
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByEndDate(LocalDate endDate);

    /**
     * 사용자별 최신 연차 내역 조회 (created_at desc 최상단)
     *
     * @param userId 사용자 ID
     * @return 최신 연차 내역
     */
    Optional<VacationHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 상태가 특정 값인 연차 내역 조회 (스케줄러용)
     *
     * @param status 상태 (R 또는 C)
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByStatus(String status);
    
    /**
     * 사용자 ID 목록으로 연차 내역 조회 (캘린더용)
     *
     * @param userIds 사용자 ID 목록
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByUserIdInOrderByStartDateAsc(List<Long> userIds);
    
}

