package com.vacation.api.domain.expense.repository;

import com.vacation.api.domain.expense.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 개인 비용 청구 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {

    /**
     * 사용자 ID로 개인 비용 청구 목록 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @return 개인 비용 청구 목록
     */
    List<ExpenseClaim> findByUserIdOrderBySeqDesc(Long userId);

    /**
     * 시퀀스와 사용자 ID로 개인 비용 청구 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 개인 비용 청구 정보
     */
    Optional<ExpenseClaim> findBySeqAndUserId(Long seq, Long userId);
    
    /**
     * 사용자 ID로 개인 비용 청구 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    @Query("SELECT COUNT(e) FROM ExpenseClaim e WHERE e.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자 ID로 개인 비용 청구 목록 조회 (페이징, 최신순)
     *
     * @param userId 사용자 ID
     * @param offset 시작 위치
     * @param limit 개수
     * @return 개인 비용 청구 목록
     */
    @Query(value = "SELECT * FROM tbl_expense_claim WHERE user_id = :userId ORDER BY seq DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<ExpenseClaim> findByUserIdOrderBySeqDescWithPaging(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 권한별 승인 대기 목록 조회 (팀장: A, AM / 본부장: B / 관리자: 전체)
     *
     * @param userIds 사용자 ID 목록
     * @param approvalStatuses 승인 상태 목록
     * @return 개인 비용 청구 목록
     */
    List<ExpenseClaim> findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
            List<Long> userIds, List<String> approvalStatuses);
    
    /**
     * 승인 상태 목록으로 개인 비용 청구 조회 (관리자용, 생성일 내림차순)
     *
     * @param approvalStatuses 승인 상태 목록
     * @return 개인 비용 청구 목록
     */
    List<ExpenseClaim> findByApprovalStatusInOrderByCreatedAtDesc(List<String> approvalStatuses);
    
    /**
     * 승인 상태가 null인 개인 비용 청구 조회 (생성일 내림차순)
     *
     * @return 개인 비용 청구 목록
     */
    List<ExpenseClaim> findByApprovalStatusIsNullOrderByCreatedAtDesc();
    
    /**
     * 사용자 ID와 청구 년월로 개인 비용 청구 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param billingYyMonth 청구 년월 (YYYYMM 형식)
     * @return 존재 여부
     */
    boolean existsByUserIdAndBillingYyMonth(Long userId, Integer billingYyMonth);
}

