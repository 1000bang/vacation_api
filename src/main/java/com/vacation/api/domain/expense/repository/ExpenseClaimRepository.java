package com.vacation.api.domain.expense.repository;

import com.vacation.api.domain.expense.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

