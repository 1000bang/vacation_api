package com.vacation.api.domain.rental.repository;

import com.vacation.api.domain.rental.entity.RentalSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 월세 지원 신청 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Repository
public interface RentalSupportRepository extends JpaRepository<RentalSupport, Long> {

    /**
     * 사용자 ID로 월세 지원 신청 목록 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @return 월세 지원 신청 목록
     */
    List<RentalSupport> findByUserIdOrderBySeqDesc(Long userId);

    /**
     * 시퀀스와 사용자 ID로 월세 지원 신청 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 지원 신청 정보
     */
    Optional<RentalSupport> findBySeqAndUserId(Long seq, Long userId);
    
    /**
     * 사용자 ID로 월세 지원 신청 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    @Query("SELECT COUNT(r) FROM RentalSupport r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자 ID로 월세 지원 신청 목록 조회 (페이징, 최신순)
     *
     * @param userId 사용자 ID
     * @param offset 시작 위치
     * @param limit 개수
     * @return 월세 지원 신청 목록
     */
    @Query(value = "SELECT * FROM tbl_rental_support WHERE user_id = :userId ORDER BY seq DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<RentalSupport> findByUserIdOrderBySeqDescWithPaging(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 권한별 승인 대기 목록 조회 (팀장: A, AM / 본부장: B / 관리자: 전체)
     *
     * @param userIds 사용자 ID 목록
     * @param approvalStatuses 승인 상태 목록
     * @return 월세 지원 신청 목록
     */
    List<RentalSupport> findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
            List<Long> userIds, List<String> approvalStatuses);
    
    /**
     * 승인 상태 목록으로 월세 지원 신청 조회 (관리자용, 생성일 내림차순)
     *
     * @param approvalStatuses 승인 상태 목록
     * @return 월세 지원 신청 목록
     */
    List<RentalSupport> findByApprovalStatusInOrderByCreatedAtDesc(List<String> approvalStatuses);
    
    /**
     * 승인 상태가 null인 월세 지원 신청 조회 (생성일 내림차순)
     *
     * @return 월세 지원 신청 목록
     */
    List<RentalSupport> findByApprovalStatusIsNullOrderByCreatedAtDesc();
    
    /**
     * 사용자 ID와 청구 년월로 월세 지원 신청 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param billingYyMonth 청구 년월 (YYYYMM 형식)
     * @return 존재 여부
     */
    boolean existsByUserIdAndBillingYyMonth(Long userId, Integer billingYyMonth);
    
    /**
     * 사용자 ID와 청구 년월로 월세 지원 신청 존재 여부 확인 (특정 seq 제외)
     * 수정 시 자기 자신을 제외한 중복 체크용
     *
     * @param userId 사용자 ID
     * @param billingYyMonth 청구 년월 (YYYYMM 형식)
     * @param seq 제외할 시퀀스
     * @return 존재 여부
     */
    boolean existsByUserIdAndBillingYyMonthAndSeqNot(Long userId, Integer billingYyMonth, Long seq);
}

