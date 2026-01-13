package com.vacation.api.domain.approval.repository;

import com.vacation.api.domain.approval.entity.ApprovalRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 승인 반려 사유 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-10
 */
@Repository
public interface ApprovalRejectionRepository extends JpaRepository<ApprovalRejection, Long> {

    /**
     * 신청 타입과 시퀀스로 반려 사유 조회
     *
     * @param applicationType 신청 타입 (VACATION, EXPENSE, RENTAL)
     * @param applicationSeq 신청 시퀀스
     * @return 반려 사유 목록
     */
    List<ApprovalRejection> findByApplicationTypeAndApplicationSeqOrderByCreatedAtDesc(
            String applicationType, Long applicationSeq);

    /**
     * 신청 타입과 시퀀스로 최신 반려 사유 조회
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @return 최신 반려 사유
     */
    Optional<ApprovalRejection> findFirstByApplicationTypeAndApplicationSeqOrderByCreatedAtDesc(
            String applicationType, Long applicationSeq);
}
