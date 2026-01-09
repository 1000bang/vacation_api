package com.vacation.api.domain.rental.repository;

import com.vacation.api.domain.rental.entity.RentalApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 월세 지원 신청 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Repository
public interface RentalApprovalRepository extends JpaRepository<RentalApproval, Long> {

    /**
     * 사용자 ID로 월세 지원 정보 목록 조회
     *
     * @param userId 사용자 ID
     * @return 월세 지원 정보 목록
     */
    List<RentalApproval> findByUserIdOrderBySeqDesc(Long userId);

    /**
     * 시퀀스와 사용자 ID로 월세 지원 정보 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 지원 정보
     */
    Optional<RentalApproval> findBySeqAndUserId(Long seq, Long userId);
}

