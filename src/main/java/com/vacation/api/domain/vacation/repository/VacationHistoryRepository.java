package com.vacation.api.domain.vacation.repository;

import com.vacation.api.domain.vacation.entity.VacationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * 사용자 ID로 연차 내역 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    @Query("SELECT COUNT(v) FROM VacationHistory v WHERE v.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자 ID로 연차 내역 목록 조회 (페이징, 최신순)
     *
     * @param userId 사용자 ID
     * @param offset 시작 위치
     * @param limit 개수
     * @return 연차 내역 목록
     */
    @Query(value = "SELECT * FROM tbl_vacation_history WHERE user_id = :userId ORDER BY seq DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<VacationHistory> findByUserIdOrderBySeqDescWithPaging(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 권한별 승인 대기 목록 조회 (팀장: A, AM / 본부장: B / 관리자: 전체)
     *
     * @param userIds 사용자 ID 목록
     * @param approvalStatuses 승인 상태 목록
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
            List<Long> userIds, List<String> approvalStatuses);
    
    /**
     * 승인 상태 목록으로 연차 내역 조회 (관리자용, 생성일 내림차순)
     *
     * @param approvalStatuses 승인 상태 목록
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByApprovalStatusInOrderByCreatedAtDesc(List<String> approvalStatuses);
    
    /**
     * 승인 상태가 null인 연차 내역 조회 (생성일 내림차순)
     *
     * @return 연차 내역 목록
     */
    List<VacationHistory> findByApprovalStatusIsNullOrderByCreatedAtDesc();
    
    /**
     * 사용자 ID와 시작일로 연차 내역 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param startDate 시작일
     * @return 존재 여부
     */
    boolean existsByUserIdAndStartDate(Long userId, LocalDate startDate);
    
}

