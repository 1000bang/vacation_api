package com.vacation.api.domain.rental.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalApprovalRepository;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
import com.vacation.api.domain.rental.request.RentalApprovalRequest;
import com.vacation.api.domain.rental.request.RentalSupportRequest;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.enums.PaymentType;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.vo.RentalSupportApplicationVO;
import com.vacation.api.vo.RentalSupportProposalVO;
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
    private final UserRepository userRepository;
    private final RentalSupportRepository rentalSupportRepository;
    private final AlarmService alarmService;

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
     * @param requesterId 요청자 사용자 ID
     * @return 월세 지원 정보 (없으면 null)
     */
    public RentalApproval getRentalSupport(Long seq, Long requesterId) {
        log.info("월세 지원 정보 조회: seq={}, requesterId={}", seq, requesterId);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        
        if ("ma".equals(authVal)) {
            // 관리자(ma)는 모든 월세 지원 품의서 조회 가능
            return rentalApprovalRepository.findById(seq)
                    .orElse(null);
        } else if ("bb".equals(authVal)) {
            // 본부장(bb)은 자신의 본부만 모든 월세 지원 품의서 조회 가능
            RentalApproval rentalApproval = rentalApprovalRepository.findById(seq)
                    .orElse(null);
            if (rentalApproval != null) {
                User applicant = userRepository.findById(rentalApproval.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision())) {
                    return rentalApproval;
                }
            }
            return null;
        } else if ("tj".equals(authVal)) {
            // 팀장(tj)은 자신의 팀만 모든 월세 지원 품의서 조회 가능
            RentalApproval rentalApproval = rentalApprovalRepository.findById(seq)
                    .orElse(null);
            if (rentalApproval != null) {
                User applicant = userRepository.findById(rentalApproval.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision()) 
                    && requester.getTeam().equals(applicant.getTeam())) {
                    return rentalApproval;
                }
            }
            return null;
        } else {
            // 일반 사용자는 본인 신청 내역만 조회 가능
            return rentalApprovalRepository.findBySeqAndUserId(seq, requesterId)
                    .orElse(null);
        }
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
        
        // 사용자 정보 조회 (권한 확인용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        // user_id당 하나만 신청 가능 (count > 1이면 반환)
        long existingCount = rentalApprovalRepository.countByUserId(userId);
        if (existingCount > 0) {
            log.warn("월세지원 품의서가 이미 존재함: userId={}, count={}", userId, existingCount);
            throw new ApiException(ApiErrorCode.DUPLICATE_RENTAL_APPROVAL);
        }
        
        // 권한에 따른 초기 approvalStatus 설정
        String initialApprovalStatus = "A"; // 기본값: 일반 사용자
        String authVal = user.getAuthVal();
        if ("tj".equals(authVal)) {
            // 팀장 권한: B (팀장 승인)로 시작
            initialApprovalStatus = "B";
        } else if ("bb".equals(authVal)) {
            // 본부장 권한: C (본부장 승인)로 시작
            initialApprovalStatus = "C";
        }
        
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
                .approvalStatus(initialApprovalStatus)
                .build();
        
        RentalApproval saved = rentalApprovalRepository.save(rentalApproval);
        log.info("월세 지원 정보 생성 완료: seq={}, userId={}, approvalStatus={}", saved.getSeq(), userId, saved.getApprovalStatus());
        
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
        rentalApproval.setApprovalStatus("AM"); // 수정 시 무조건 AM 상태로 변경
        
        RentalApproval updated = rentalApprovalRepository.save(rentalApproval);
        log.info("월세 지원 정보 수정 완료: seq={}, userId={}, approvalStatus={}", seq, userId, updated.getApprovalStatus());
        
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
     * 월세 지원 신청 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 월세 지원 신청 목록
     */
    public List<RentalSupport> getRentalSupportApplicationList(Long userId, int page, int size) {
        log.info("월세 지원 신청 목록 조회: userId={}, page={}, size={}", userId, page, size);
        int offset = page * size;
        return rentalSupportRepository.findByUserIdOrderBySeqDescWithPaging(userId, offset, size);
    }
    
    /**
     * 월세 지원 신청 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    public long getRentalSupportApplicationCount(Long userId) {
        log.info("월세 지원 신청 총 개수 조회: userId={}", userId);
        return rentalSupportRepository.countByUserId(userId);
    }

    /**
     * 월세 지원 신청 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 월세 지원 신청 정보 (없으면 null)
     */
    public RentalSupport getRentalSupportApplication(Long seq, Long requesterId) {
        log.info("월세 지원 신청 조회: seq={}, requesterId={}", seq, requesterId);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        
        if ("ma".equals(authVal)) {
            // 관리자(ma)는 모든 월세 지원 신청 조회 가능
            return rentalSupportRepository.findById(seq)
                    .orElse(null);
        } else if ("bb".equals(authVal)) {
            // 본부장(bb)은 자신의 본부만 모든 월세 지원 신청 조회 가능
            RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                    .orElse(null);
            if (rentalSupport != null) {
                User applicant = userRepository.findById(rentalSupport.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision())) {
                    return rentalSupport;
                }
            }
            return null;
        } else if ("tj".equals(authVal)) {
            // 팀장(tj)은 자신의 팀만 모든 월세 지원 신청 조회 가능
            RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                    .orElse(null);
            if (rentalSupport != null) {
                User applicant = userRepository.findById(rentalSupport.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision()) 
                    && requester.getTeam().equals(applicant.getTeam())) {
                    return rentalSupport;
                }
            }
            return null;
        } else {
            // 일반 사용자는 본인 신청 내역만 조회 가능
            return rentalSupportRepository.findBySeqAndUserId(seq, requesterId)
                    .orElse(null);
        }
    }

    /**
     * 월세 지원 신청 조회 (seq만으로 조회, 권한 체크 없음)
     *
     * @param seq 시퀀스
     * @return 월세 지원 신청 정보 (없으면 null)
     */
    public RentalSupport getRentalSupportApplicationById(Long seq) {
        log.info("월세 지원 신청 조회: seq={}", seq);
        return rentalSupportRepository.findById(seq)
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
        
        // 사용자 정보 조회 (권한 확인용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        // 청구 기간 및 납입일 계산 (requestDate의 연도 기준)
        LocalDate requestDate = request.getRequestDate();
        int requestYear = requestDate.getYear();
        int requestMonth = requestDate.getMonthValue(); // 신청일자의 월 (1-12)
        
        // 청구 년월 계산 (YYYYMM 형식)
        int billingYyMonth = com.vacation.api.util.BillingUtil.calculateBillingYyMonth(
                requestDate, 
                request.getMonth()
        );
        
        // 같은 월에 월세지원 신청이 이미 존재하는지 확인
        if (rentalSupportRepository.existsByUserIdAndBillingYyMonth(userId, billingYyMonth)) {
            log.warn("해당 월에 월세지원 신청이 이미 존재함: userId={}, billingYyMonth={}", userId, billingYyMonth);
            throw new ApiException(ApiErrorCode.DUPLICATE_RENTAL_MONTH);
        }
        
        // 권한에 따른 초기 approvalStatus 설정
        String initialApprovalStatus = "A"; // 기본값: 일반 사용자
        String authVal = user.getAuthVal();
        if ("tj".equals(authVal)) {
            // 팀장 권한: B (팀장 승인)로 시작
            initialApprovalStatus = "B";
        } else if ("bb".equals(authVal)) {
            // 본부장 권한: C (본부장 승인)로 시작
            initialApprovalStatus = "C";
        }
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
                .approvalStatus(initialApprovalStatus) // 권한에 따라 초기 상태 설정 (tj: B, bb: C, 일반: A)
                .build();
        
        RentalSupport saved = rentalSupportRepository.save(rentalSupport);
        
        // 알람 생성: 팀장에게
        alarmService.createApplicationCreatedAlarm(userId, "RENTAL", saved.getSeq());
        
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
        int billingYyMonth = com.vacation.api.util.BillingUtil.calculateBillingYyMonth(
                requestDate, 
                request.getMonth()
        );
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
        // 수정 시 무조건 AM 상태로 변경
        rentalSupport.setApprovalStatus("AM"); // 수정됨
        
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

    /**
     * 월세 지원 청구서 문서 생성용 VO 생성
     *
     * @param rentalSupport 월세 지원 신청 정보
     * @param user 사용자 정보
     * @return RentalSupportApplicationVO
     */
    public RentalSupportApplicationVO createRentalSupportApplicationVO(
            RentalSupport rentalSupport,
            com.vacation.api.domain.user.entity.User user) {
        log.info("월세 지원 청구서 문서 VO 생성: seq={}, userId={}", rentalSupport.getSeq(), rentalSupport.getUserId());
        
        String department = user.getDivision() + "/" + user.getTeam();
        int month = rentalSupport.getBillingYyMonth() % 100;
        
        return RentalSupportApplicationVO.builder()
                .requestDate(rentalSupport.getRequestDate())
                .month(month)
                .department(department)
                .applicant(user.getName())
                .contractStartDate(rentalSupport.getContractStartDate())
                .contractEndDate(rentalSupport.getContractEndDate())
                .contractMonthlyRent(rentalSupport.getContractMonthlyRent())
                .paymentType(rentalSupport.getPaymentType())
                .billingStartDate(rentalSupport.getBillingStartDate())
                .billingPeriodStartDate(rentalSupport.getBillingPeriodStartDate())
                .billingPeriodEndDate(rentalSupport.getBillingPeriodEndDate())
                .paymentDate(rentalSupport.getPaymentDate())
                .paymentAmount(rentalSupport.getPaymentAmount())
                .billingAmount(rentalSupport.getBillingAmount())
                .build();
    }

    /**
     * 월세 지원 품의서 문서 생성용 VO 생성
     *
     * @param rentalApproval 월세 품의 정보
     * @param user 사용자 정보
     * @return RentalSupportProposalVO
     */
    public RentalSupportProposalVO createRentalSupportProposalVO(
            RentalApproval rentalApproval,
            com.vacation.api.domain.user.entity.User user) {
        log.info("월세 지원 품의서 문서 VO 생성: seq={}, userId={}", rentalApproval.getSeq(), rentalApproval.getUserId());
        
        String department = user.getDivision() + "/" + user.getTeam();
        
        return RentalSupportProposalVO.builder()
                .requestDate(LocalDate.now()) // 품의서는 현재 날짜 사용
                .department(department)
                .applicant(user.getName())
                .currentAddress(rentalApproval.getPreviousAddress())
                .rentalAddress(rentalApproval.getRentalAddress())
                .contractStartDate(rentalApproval.getContractStartDate())
                .contractEndDate(rentalApproval.getContractEndDate())
                .contractMonthlyRent(rentalApproval.getContractMonthlyRent())
                .billingAmount(rentalApproval.getBillingAmount())
                .billingStartDate(rentalApproval.getBillingStartDate())
                .reason(rentalApproval.getBillingReason())
                .build();
    }
}

