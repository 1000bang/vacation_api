package com.vacation.api.domain.rental.repository;

import com.vacation.api.domain.rental.entity.RentalProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 월세 품의 신청 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Repository
public interface RentalProposalRepository extends JpaRepository<RentalProposal, Long> {

    /**
     * 사용자 ID로 월세 품의 정보 목록 조회
     *
     * @param userId 사용자 ID
     * @return 월세 품의 정보 목록
     */
    List<RentalProposal> findByUserIdOrderBySeqDesc(Long userId);

    /**
     * 시퀀스와 사용자 ID로 월세 품의 정보 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 품의 정보
     */
    Optional<RentalProposal> findBySeqAndUserId(Long seq, Long userId);

    /**
     * 사용자 ID 목록과 승인 상태 목록으로 월세 품의 정보 목록 조회 (생성일 내림차순)
     *
     * @param userIds 사용자 ID 목록
     * @param approvalStatuses 승인 상태 목록
     * @return 월세 품의 정보 목록
     */
    List<RentalProposal> findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(List<Long> userIds, List<String> approvalStatuses);
    
    /**
     * 승인 상태 목록으로 월세 품의서 조회 (관리자용, 생성일 내림차순)
     *
     * @param approvalStatuses 승인 상태 목록
     * @return 월세 품의서 목록
     */
    List<RentalProposal> findByApprovalStatusInOrderByCreatedAtDesc(List<String> approvalStatuses);
    
    /**
     * 승인 상태가 null인 월세 품의서 조회 (생성일 내림차순)
     *
     * @return 월세 품의서 목록
     */
    List<RentalProposal> findByApprovalStatusIsNullOrderByCreatedAtDesc();
    
    /**
     * 사용자 ID로 월세 품의서 개수 조회
     *
     * @param userId 사용자 ID
     * @return 개수
     */
    long countByUserId(Long userId);
}
