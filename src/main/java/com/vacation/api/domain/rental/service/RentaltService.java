package com.vacation.api.domain.rental.service;

import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalApprovalRepository;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
import com.vacation.api.domain.rental.request.RentalApprovalRequest;
import com.vacation.api.domain.rental.request.RentalSupportRequest;
import com.vacation.api.enums.PaymentType;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.List;

/**
 * 월세 지원 신청 정보 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RentaltService {

    private final RentalApprovalRepository rentalApprovalRepository;
    private final RentalSupportRepository rentalSupportRepository;

    /**
     * 월세 지원 정보 목록 조회
     *
     * @param userId 사용자 ID
     * @return 월세 지원 정보 목록
     */
    public List<RentalApproval> getRentalSupportList(Long userId) {
        log.info("월세 지원 정보 목록 조회: userId={}", userId);
        return rentalApprovalRepository.findByUserIdOrderBySeqDesc(userId);
    }

    /**
     * 월세 지원 정보 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 지원 정보 (없으면 null)
     */
    public RentalApproval getRentalSupport(Long seq, Long userId) {
        log.info("월세 지원 정보 조회: seq={}, userId={}", seq, userId);
        return rentalApprovalRepository.findBySeqAndUserId(seq, userId)
                .orElse(null);
    }

    /**
     * 월세 지원 정보 생성
     *
     * @param userId 사용자 ID
     * @param request 월세 지원 정보 요청 데이터
     * @return 생성된 월세 지원 정보
     */
    @Transactional
    public RentalApproval createRentalSupport(Long userId, RentalApprovalRequest request) {
        log.info("월세 지원 정보 생성: userId={}", userId);
        
        RentalApproval rentalApproval = RentalApproval.builder()
                .userId(userId)
                .previousAddress(request.getPreviousAddress())
                .rentalAddress(request.getRentalAddress())
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .contractMonthlyRent(request.getContractMonthlyRent())
                .billingAmount(request.getBillingAmount())
                .billingStartDate(request.getBillingStartDate())
                .billingReason(request.getBillingReason())
                .build();
        
        RentalApproval saved = rentalApprovalRepository.save(rentalApproval);
        log.info("월세 지원 정보 생성 완료: seq={}, userId={}", saved.getSeq(), userId);
        
        return saved;
    }

    /**
     * 월세 지원 정보 수정
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @param request 월세 지원 정보 요청 데이터
     * @return 수정된 월세 지원 정보
     */
    @Transactional
    public RentalApproval updateRentalSupport(Long seq, Long userId, RentalApprovalRequest request) {
        log.info("월세 지원 정보 수정: seq={}, userId={}", seq, userId);
        
        RentalApproval rentalApproval = rentalApprovalRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 정보: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });
        
        rentalApproval.setPreviousAddress(request.getPreviousAddress());
        rentalApproval.setRentalAddress(request.getRentalAddress());
        rentalApproval.setContractStartDate(request.getContractStartDate());
        rentalApproval.setContractEndDate(request.getContractEndDate());
        rentalApproval.setContractMonthlyRent(request.getContractMonthlyRent());
        rentalApproval.setBillingAmount(request.getBillingAmount());
        rentalApproval.setBillingStartDate(request.getBillingStartDate());
        rentalApproval.setBillingReason(request.getBillingReason());
        
        RentalApproval updated = rentalApprovalRepository.save(rentalApproval);
        log.info("월세 지원 정보 수정 완료: seq={}, userId={}", seq, userId);
        
        return updated;
    }

    /**
     * 월세 지원 정보 삭제
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteRentalSupport(Long seq, Long userId) {
        log.info("월세 지원 정보 삭제: seq={}, userId={}", seq, userId);
        
        RentalApproval rentalApproval = rentalApprovalRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 정보: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });
        
        rentalApprovalRepository.delete(rentalApproval);
        log.info("월세 지원 정보 삭제 완료: seq={}, userId={}", seq, userId);
    }

    // ========== 월세 지원 신청 (청구서) 관련 메서드 ==========

    /**
     * 월세 지원 신청 목록 조회
     *
     * @param userId 사용자 ID
     * @return 월세 지원 신청 목록
     */
    public List<RentalSupport> getRentalSupportApplicationList(Long userId) {
        log.info("월세 지원 신청 목록 조회: userId={}", userId);
        return rentalSupportRepository.findByUserIdOrderBySeqDesc(userId);
    }

    /**
     * 월세 지원 신청 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 지원 신청 정보 (없으면 null)
     */
    public RentalSupport getRentalSupportApplication(Long seq, Long userId) {
        log.info("월세 지원 신청 조회: seq={}, userId={}", seq, userId);
        return rentalSupportRepository.findBySeqAndUserId(seq, userId)
                .orElse(null);
    }

    /**
     * 월세 지원 신청 생성
     *
     * @param userId 사용자 ID
     * @param request 월세 지원 신청 요청 데이터
     * @return 생성된 월세 지원 신청 정보
     */
    @Transactional
    public RentalSupport createRentalSupportApplication(Long userId, RentalSupportRequest request) {
        log.info("월세 지원 신청 생성: userId={}", userId);
        
        // 청구 기간 및 납입일 계산 (requestDate의 연도 기준)
        LocalDate requestDate = request.getRequestDate();
        int requestYear = requestDate.getYear();
        int requestMonth = requestDate.getMonthValue(); // 신청일자의 월 (1-12)
        
        // 청구 년월 계산 (YYYYMM 형식)
        // requestDate가 1월이고 month가 12이면 전년도 12월로 설정
        int month = request.getMonth();
        int year = requestYear;
        // requestDate가 1월이고 month가 12이면 전년도 사용
        if (requestMonth == 1 && month == 12) {
            year = requestYear - 1;
        }
        int billingYyMonth = year * 100 + month;
        int contractDay = request.getContractStartDate().getDayOfMonth();
        int billingMonth = request.getMonth();
        
        // 청구월이 신청일자의 월보다 크면 전년도 기준 (예: 신청일자 1월, 청구월 12월 → 전년도 12월)
        int baseYear = requestYear;
        if (billingMonth > requestMonth) {
            baseYear = requestYear - 1;
        }
        
        // 청구월세 시작일: (청구월 - 1)월의 계약일자
        int startYear = baseYear;
        int startMonth = billingMonth - 1;
        if (startMonth == 0) {
            startMonth = 12;
            startYear = baseYear - 1;
        }
        LocalDate billingPeriodStartDate = LocalDate.of(startYear, startMonth, contractDay);
        
        // 청구월세 종료일: 시작일 + 한달 - 1일
        LocalDate billingPeriodEndDate = billingPeriodStartDate.plusMonths(1).minusDays(1);
        
        // 월세 납입일 계산
        LocalDate paymentDate;
        if (request.getPaymentType() == PaymentType.POSTPAID) {
            // 후불: 청구월세 종료일 + 1일
            paymentDate = billingPeriodEndDate.plusDays(1);
        } else {
            // 선불: 청구월세 시작일 - 1일
            paymentDate = billingPeriodStartDate.minusDays(1);
        }
        
        RentalSupport rentalSupport = RentalSupport.builder()
                .userId(userId)
                .requestDate(request.getRequestDate())
                .billingYyMonth(billingYyMonth)
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .contractMonthlyRent(request.getContractMonthlyRent())
                .paymentType(request.getPaymentType())
                .billingStartDate(request.getBillingStartDate())
                .billingPeriodStartDate(billingPeriodStartDate)
                .billingPeriodEndDate(billingPeriodEndDate)
                .paymentDate(paymentDate)
                .paymentAmount(request.getPaymentAmount())
                .billingAmount(request.getBillingAmount())
                .build();
        
        RentalSupport saved = rentalSupportRepository.save(rentalSupport);
        log.info("월세 지원 신청 생성 완료: seq={}, userId={}", saved.getSeq(), userId);
        
        return saved;
    }

    /**
     * 월세 지원 신청 수정
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @param request 월세 지원 신청 요청 데이터
     * @return 수정된 월세 지원 신청 정보
     */
    @Transactional
    public RentalSupport updateRentalSupportApplication(Long seq, Long userId, RentalSupportRequest request) {
        log.info("월세 지원 신청 수정: seq={}, userId={}", seq, userId);
        
        RentalSupport rentalSupport = rentalSupportRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });
        
        // 청구 기간 및 납입일 계산 (requestDate의 연도 기준)
        LocalDate requestDate = request.getRequestDate();
        int requestYear = requestDate.getYear();
        int requestMonth = requestDate.getMonthValue(); // 신청일자의 월 (1-12)
        
        // 청구 년월 계산 (YYYYMM 형식)
        // requestDate가 1월이고 month가 12이면 전년도 12월로 설정
        int month = request.getMonth();
        int year = requestYear;
        // requestDate가 1월이고 month가 12이면 전년도 사용
        if (requestMonth == 1 && month == 12) {
            year = requestYear - 1;
        }
        int billingYyMonth = year * 100 + month;
        int contractDay = request.getContractStartDate().getDayOfMonth();
        int billingMonth = request.getMonth();
        
        // 청구월이 신청일자의 월보다 크면 전년도 기준 (예: 신청일자 1월, 청구월 12월 → 전년도 12월)
        int baseYear = requestYear;
        if (billingMonth > requestMonth) {
            baseYear = requestYear - 1;
        }
        
        // 청구월세 시작일: (청구월 - 1)월의 계약일자
        int startYear = baseYear;
        int startMonth = billingMonth - 1;
        if (startMonth == 0) {
            startMonth = 12;
            startYear = baseYear - 1;
        }
        LocalDate billingPeriodStartDate = LocalDate.of(startYear, startMonth, contractDay);
        
        // 청구월세 종료일: 시작일 + 한달 - 1일
        LocalDate billingPeriodEndDate = billingPeriodStartDate.plusMonths(1).minusDays(1);
        
        // 월세 납입일 계산
        LocalDate paymentDate;
        if (request.getPaymentType() == PaymentType.POSTPAID) {
            // 후불: 청구월세 종료일 + 1일
            paymentDate = billingPeriodEndDate.plusDays(1);
        } else {
            // 선불: 청구월세 시작일 - 1일
            paymentDate = billingPeriodStartDate.minusDays(1);
        }
        
        rentalSupport.setRequestDate(request.getRequestDate());
        rentalSupport.setBillingYyMonth(billingYyMonth);
        rentalSupport.setContractStartDate(request.getContractStartDate());
        rentalSupport.setContractEndDate(request.getContractEndDate());
        rentalSupport.setContractMonthlyRent(request.getContractMonthlyRent());
        rentalSupport.setPaymentType(request.getPaymentType());
        rentalSupport.setBillingStartDate(request.getBillingStartDate());
        rentalSupport.setBillingPeriodStartDate(billingPeriodStartDate);
        rentalSupport.setBillingPeriodEndDate(billingPeriodEndDate);
        rentalSupport.setPaymentDate(paymentDate);
        rentalSupport.setPaymentAmount(request.getPaymentAmount());
        rentalSupport.setBillingAmount(request.getBillingAmount());
        
        RentalSupport updated = rentalSupportRepository.save(rentalSupport);
        log.info("월세 지원 신청 수정 완료: seq={}, userId={}", seq, userId);
        
        return updated;
    }

    /**
     * 월세 지원 신청 삭제
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteRentalSupportApplication(Long seq, Long userId) {
        log.info("월세 지원 신청 삭제: seq={}, userId={}", seq, userId);
        
        RentalSupport rentalSupport = rentalSupportRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });
        
        rentalSupportRepository.delete(rentalSupport);
        log.info("월세 지원 신청 삭제 완료: seq={}, userId={}", seq, userId);
    }
}

