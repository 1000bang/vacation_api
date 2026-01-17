package com.vacation.api.domain.vacation.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.domain.approval.entity.ApprovalRejection;
import com.vacation.api.domain.approval.repository.ApprovalRejectionRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.UserVacationInfoRepository;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import com.vacation.api.domain.vacation.request.UpdateVacationInfoRequest;
import com.vacation.api.domain.vacation.request.VacationRequest;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.util.ApprovalStatusResolver;
import com.vacation.api.vo.VacationDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 연차 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VacationService {

    private final UserVacationInfoRepository userVacationInfoRepository;
    private final VacationHistoryRepository vacationHistoryRepository;
    private final UserRepository userRepository;
    private final AlarmService alarmService;
    private final ApprovalRejectionRepository approvalRejectionRepository;
    private final ApprovalStatusResolver approvalStatusResolver;

    /**
     * 사용자별 연차 정보 조회
     *
     * @param userId 사용자 ID
     * @return 연차 정보 (없으면 0.0으로 초기화된 정보 반환)
     */
    public UserVacationInfo getUserVacationInfo(Long userId) {
        log.info("사용자별 연차 정보 조회: userId={}", userId);
        
        return userVacationInfoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("연차 정보가 없어 기본값 반환: userId={}", userId);
                    // 연차 정보가 없으면 0.0으로 초기화된 정보 반환 (DB에 저장하지 않음)
                    return UserVacationInfo.builder()
                            .userId(userId)
                            .annualVacationDays(0.0)
                            .usedVacationDays(0.0)
                            .reservedVacationDays(0.0)
                            .build();
                });
    }
    
    /**
     * 사용자별 연차 정보 수정
     *
     * @param userId 사용자 ID
     * @param request 연차 정보 수정 요청 데이터
     * @return 수정된 연차 정보
     */
    @Transactional
    public UserVacationInfo updateUserVacationInfo(Long userId, UpdateVacationInfoRequest request) {
        log.info("사용자별 연차 정보 수정: userId={}, annual={}, used={}, reserved={}", 
                userId, request.getAnnualVacationDays(), request.getUsedVacationDays(), request.getReservedVacationDays());
        
        UserVacationInfo vacationInfo = userVacationInfoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("연차 정보가 없어 새로 생성: userId={}", userId);
                    return UserVacationInfo.builder()
                            .userId(userId)
                            .annualVacationDays(0.0)
                            .usedVacationDays(0.0)
                            .reservedVacationDays(0.0)
                            .build();
                });
        
        if (request.getAnnualVacationDays() != null) {
            vacationInfo.setAnnualVacationDays(request.getAnnualVacationDays());
        }
        if (request.getUsedVacationDays() != null) {
            vacationInfo.setUsedVacationDays(request.getUsedVacationDays());
        }
        if (request.getReservedVacationDays() != null) {
            vacationInfo.setReservedVacationDays(request.getReservedVacationDays());
        }
        
        UserVacationInfo saved = userVacationInfoRepository.save(vacationInfo);
        
        // 연차 정보 수정 시 firstLogin을 false로 변경
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        if (user.getFirstLogin()) {
            user.setFirstLogin(false);
            userRepository.save(user);
            log.info("최초 로그인 상태 해제: userId={}", userId);
        }
        
        log.info("연차 정보 수정 완료: userId={}", userId);
        
        return saved;
    }

    /**
     * 연차 내역 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 연차 내역 목록
     */
    public List<VacationHistory> getVacationHistoryList(Long userId, int page, int size) {
        log.info("연차 내역 목록 조회: userId={}, page={}, size={}", userId, page, size);
        int offset = page * size;
        return vacationHistoryRepository.findByUserIdOrderBySeqDescWithPaging(userId, offset, size);
    }
    
    /**
     * 연차 내역 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    public long getVacationHistoryCount(Long userId) {
        log.info("연차 내역 총 개수 조회: userId={}", userId);
        return vacationHistoryRepository.countByUserId(userId);
    }

    /**
     * 본부 전체 캘린더용 휴가 목록 조회 (권한 무관)
     * 현재 월 기준 전후 1개월 범위의 휴가만 조회
     *
     * @param userId 요청자 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월 (1-12)
     * @return 휴가 내역 목록
     */
    public List<VacationHistory> getCalendarVacationList(Long userId, Integer year, Integer month) {
        log.info("캘린더용 휴가 목록 조회: userId={}, year={}, month={}", userId, year, month);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.USER_NOT_FOUND);
                });

        // 본부 전체 사용자 조회 (권한 무관)
        String division = requester.getDivision();
        log.info("본부 전체 휴가 조회: 본부={}", division);
        
        // 현재 월 기준 전후 1개월 범위 계산
        LocalDate currentMonthStart = LocalDate.of(year, month, 1);
        LocalDate prevMonthStart = currentMonthStart.minusMonths(1);
        LocalDate nextMonthEnd = currentMonthStart.plusMonths(2).minusDays(1);
        
        // QueryDSL 조인을 사용하여 본부와 날짜 범위로 한 번에 휴가 조회
        List<VacationHistory> vacationList = vacationHistoryRepository
                .findByDivisionAndDateRange(division, 
                        List.of(AuthVal.MASTER.getCode(), AuthVal.DIVISION_HEAD.getCode(), 
                                AuthVal.TEAM_LEADER.getCode(), AuthVal.TEAM_MEMBER.getCode()), 
                        prevMonthStart, nextMonthEnd);
        
        log.info("캘린더용 휴가 목록 조회 완료: userId={}, 본부={}, 범위={}~{}, count={}", 
                userId, division, prevMonthStart, nextMonthEnd, vacationList.size());
        return vacationList;
    }

    /**
     * 연차 내역 조회
     *
     * @param seq 시퀀스
     * @param requesterId 요청자 사용자 ID
     * @return 연차 내역 (없으면 null)
     */
    public VacationHistory getVacationHistory(Long seq, Long requesterId) {
        log.info("연차 내역 조회: seq={}, requesterId={}", seq, requesterId);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        
        if (AuthVal.MASTER.getCode().equals(authVal)) {
            // 관리자(ma)는 모든 휴가 내역 조회 가능
            return vacationHistoryRepository.findById(seq)
                    .orElse(null);
        } else if (AuthVal.DIVISION_HEAD.getCode().equals(authVal)) {
            // 본부장(bb)은 자신의 본부만 모든 휴가 내역 조회 가능
            VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                    .orElse(null);
            if (vacationHistory != null) {
                User applicant = userRepository.findById(vacationHistory.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision())) {
                    return vacationHistory;
                }
            }
            return null;
        } else if (AuthVal.TEAM_LEADER.getCode().equals(authVal)) {
            // 팀장(tj)은 자신의 팀만 모든 휴가 내역 조회 가능
            VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                    .orElse(null);
            if (vacationHistory != null) {
                User applicant = userRepository.findById(vacationHistory.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision()) 
                    && requester.getTeam().equals(applicant.getTeam())) {
                    return vacationHistory;
                }
            }
            return null;
        } else {
            // 일반 사용자는 본인 신청 내역만 조회 가능
            return vacationHistoryRepository.findBySeqAndUserId(seq, requesterId)
                    .orElse(null);
        }
    }

    /**
     * 연차 내역 조회 (seq만으로 조회, 권한 체크 없음)
     *
     * @param seq 시퀀스
     * @return 연차 내역 (없으면 null)
     */
    public VacationHistory getVacationHistoryById(Long seq) {
        log.info("연차 내역 조회: seq={}", seq);
        return vacationHistoryRepository.findById(seq)
                .orElse(null);
    }

    /**
     * 반려 사유 조회
     * created_at desc로 정렬하여 최신 반려 사유 1개만 조회
     *
     * @param seq 휴가 신청 시퀀스
     * @return 반려 사유 (없으면 null)
     */
    public String getRejectionReason(Long seq) {
        log.info("반려 사유 조회: seq={}", seq);
        // created_at desc로 정렬하여 최신 반려 사유 1개만 조회
        return approvalRejectionRepository
                .findFirstByApplicationTypeAndApplicationSeqOrderByCreatedAtDesc(ApplicationType.VACATION.getCode(), seq)
                .map(ApprovalRejection::getRejectionReason)
                .orElse(null);
    }

    /**
     * 휴가 신청
     *
     * @param userId 사용자 ID
     * @param request 휴가 신청 요청 데이터
     * @return 생성된 연차 내역
     */
    @Transactional
    public VacationHistory createVacation(Long userId, VacationRequest request) {
        log.info("휴가 신청: userId={}, startDate={}, endDate={}, period={}", 
                userId, request.getStartDate(), request.getEndDate(), request.getPeriod());

        // 사용자 정보 조회 (권한 확인용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        // 같은 날짜에 휴가 신청이 이미 존재하는지 확인
        if (vacationHistoryRepository.existsByUserIdAndStartDate(userId, request.getStartDate())) {
            log.warn("같은 날짜에 휴가 신청이 이미 존재함: userId={}, startDate={}", userId, request.getStartDate());
            throw new ApiException(ApiErrorCode.DUPLICATE_VACATION_DATE);
        }
        
        // 권한에 따른 초기 approvalStatus 설정
        String initialApprovalStatus = approvalStatusResolver.resolveInitialApprovalStatus(user.getAuthVal());

        // 사용자 연차 정보 조회 또는 생성
        UserVacationInfo vacationInfo = getUserVacationInfo(userId);

        // 잔여 연차 확인
        Double remainingDays = vacationInfo.getAnnualVacationDays() 
                - vacationInfo.getUsedVacationDays() 
                - vacationInfo.getReservedVacationDays();

        if (remainingDays < request.getPeriod()) {
            log.warn("잔여 연차 부족: userId={}, remaining={}, requested={}", 
                    userId, remainingDays, request.getPeriod());
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "잔여 연차가 부족합니다.");
        }

        // 오늘 날짜 확인
        LocalDate today = LocalDate.now();
        boolean isFuture = request.getStartDate().isAfter(today);
        
        // 직전 남은 연차 계산 (신청 시점)
        Double previousRemainingDays = vacationInfo.getAnnualVacationDays() 
                - vacationInfo.getUsedVacationDays() 
                - vacationInfo.getReservedVacationDays();
        
        // 남은 연차 계산 (신청 후)
        Double remainingVacationDays = previousRemainingDays - request.getPeriod();

        // 연차 내역 생성
        VacationHistory vacationHistory = VacationHistory.builder()
                .userId(userId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .period(request.getPeriod())
                .type(request.getVacationType())
                .reason(request.getReason())
                .requestDate(request.getRequestDate())
                .annualVacationDays(vacationInfo.getAnnualVacationDays())
                .previousRemainingDays(previousRemainingDays)
                .usedVacationDays(request.getPeriod())
                .remainingVacationDays(remainingVacationDays)
                .status(isFuture ? "R" : "C") // R: 예약중, C: 사용 연차에 카운트됨
                .approvalStatus(initialApprovalStatus) // 권한에 따라 초기 상태 설정 (tj: B, bb: C, 일반: A)
                .build();

        VacationHistory saved = vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 팀장에게
        alarmService.createApplicationCreatedAlarm(userId, ApplicationType.VACATION.getCode(), saved.getSeq());

        // 예약중 연차 업데이트 (미래 날짜인 경우)
        if (isFuture) {
            vacationInfo.setReservedVacationDays(
                    vacationInfo.getReservedVacationDays() + request.getPeriod()
            );
            userVacationInfoRepository.save(vacationInfo);
        } else {
            // 과거 또는 오늘 날짜인 경우 사용 연차로 처리
            vacationInfo.setUsedVacationDays(
                    vacationInfo.getUsedVacationDays() + request.getPeriod()
            );
            userVacationInfoRepository.save(vacationInfo);
        }

        log.info("휴가 신청 완료: seq={}, userId={}", saved.getSeq(), userId);
        return saved;
    }

    /**
     * 휴가 신청 수정
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @param request 휴가 신청 요청 데이터
     * @return 수정된 연차 내역
     */
    @Transactional
    public VacationHistory updateVacation(Long seq, Long userId, VacationRequest request) {
        log.info("휴가 신청 수정: seq={}, userId={}", seq, userId);

        VacationHistory vacationHistory = vacationHistoryRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 연차 내역: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT);
                });

        UserVacationInfo vacationInfo = getUserVacationInfo(userId);

        // 기존 연차 차감 (status에 따라)
        LocalDate today = LocalDate.now();
        String oldStatus = vacationHistory.getStatus();
        
        if ("R".equals(oldStatus)) {
            // 기존에 예약중이었던 경우
            vacationInfo.setReservedVacationDays(
                    Math.max(0, vacationInfo.getReservedVacationDays() - vacationHistory.getPeriod())
            );
        } else if ("C".equals(oldStatus)) {
            // 기존에 사용된 경우
            vacationInfo.setUsedVacationDays(
                    Math.max(0, vacationInfo.getUsedVacationDays() - vacationHistory.getPeriod())
            );
        }

        // 새로운 연차 확인
        Double remainingDays = vacationInfo.getAnnualVacationDays() 
                - vacationInfo.getUsedVacationDays() 
                - vacationInfo.getReservedVacationDays();

        if (remainingDays < request.getPeriod()) {
            log.warn("잔여 연차 부족: userId={}, remaining={}, requested={}", 
                    userId, remainingDays, request.getPeriod());
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "잔여 연차가 부족합니다.");
        }

        // 직전 남은 연차 계산 (수정 시점)
        // 수정 모드에서 연차 정보가 제공되면 사용, 아니면 자동 계산
        Double previousRemainingDays;
        Double annualVacationDays;
        Double remainingVacationDays;
        
        if (request.getPreviousRemainingDays() != null && 
            request.getAnnualVacationDays() != null && 
            request.getRemainingVacationDays() != null) {
            // 수정 모드에서 연차 정보가 제공된 경우
            previousRemainingDays = request.getPreviousRemainingDays();
            annualVacationDays = request.getAnnualVacationDays();
            remainingVacationDays = request.getRemainingVacationDays();
        } else {
            // 자동 계산: 해당 신청서 이전의 모든 신청서들을 고려하여 계산
            annualVacationDays = vacationInfo.getAnnualVacationDays();
            
            // 해당 신청서 이전의 모든 신청서들을 생성 시간 순으로 조회
            List<VacationHistory> allHistories = vacationHistoryRepository.findByUserIdOrderBySeqDesc(userId);
            LocalDateTime currentCreatedAt = vacationHistory.getCreatedAt();
            List<VacationHistory> previousHistories = allHistories.stream()
                    .filter(vh -> vh.getCreatedAt().isBefore(currentCreatedAt))
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .toList();
            
            // 이전 신청서들을 순차적으로 계산하여 previousRemainingDays 계산
            Double calculatedPreviousRemaining = annualVacationDays;
            for (VacationHistory previous : previousHistories) {
                calculatedPreviousRemaining = calculatedPreviousRemaining - previous.getPeriod();
            }
            
            previousRemainingDays = calculatedPreviousRemaining;
            remainingVacationDays = previousRemainingDays - request.getPeriod();
        }

        // 연차 내역 수정
        vacationHistory.setStartDate(request.getStartDate());
        vacationHistory.setEndDate(request.getEndDate());
        vacationHistory.setPeriod(request.getPeriod());
        vacationHistory.setType(request.getVacationType());
        vacationHistory.setReason(request.getReason());
        vacationHistory.setRequestDate(request.getRequestDate());
        vacationHistory.setAnnualVacationDays(annualVacationDays);
        vacationHistory.setPreviousRemainingDays(previousRemainingDays);
        vacationHistory.setUsedVacationDays(request.getPeriod());
        vacationHistory.setRemainingVacationDays(remainingVacationDays);
        // 수정 시 무조건 AM 상태로 변경
        vacationHistory.setApprovalStatus(com.vacation.api.enums.ApprovalStatus.MODIFIED.getName()); // 수정됨

        // 새로운 연차 추가 및 status 설정
        boolean isFuture = request.getStartDate().isAfter(today);
        if (isFuture) {
            vacationInfo.setReservedVacationDays(
                    vacationInfo.getReservedVacationDays() + request.getPeriod()
            );
            vacationHistory.setStatus("R");
        } else {
            vacationInfo.setUsedVacationDays(
                    vacationInfo.getUsedVacationDays() + request.getPeriod()
            );
            vacationHistory.setStatus("C");
        }

        VacationHistory updated = vacationHistoryRepository.save(vacationHistory);

        userVacationInfoRepository.save(vacationInfo);

        // 수정된 항목의 생성 시간 저장
        LocalDateTime modifiedCreatedAt = vacationHistory.getCreatedAt();
        
        // 수정된 항목 이후에 생성된 모든 신청서들의 remainingVacationDays 재계산
        List<VacationHistory> allHistories = vacationHistoryRepository.findByUserIdOrderBySeqDesc(userId);
        List<VacationHistory> subsequentHistories = allHistories.stream()
                .filter(vh -> vh.getCreatedAt().isAfter(modifiedCreatedAt) && !vh.getSeq().equals(seq))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // 생성 시간 순으로 정렬
                .toList();
        
        if (!subsequentHistories.isEmpty()) {
            log.info("수정 후 이후 신청서 재계산 시작: userId={}, count={}", userId, subsequentHistories.size());
            
            // 수정된 항목 이전의 모든 신청서들을 생성 시간 순으로 정렬
            List<VacationHistory> previousHistories = allHistories.stream()
                    .filter(vh -> vh.getCreatedAt().isBefore(modifiedCreatedAt) || vh.getSeq().equals(seq))
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // 생성 시간 순으로 정렬
                    .toList();
            
            // 수정된 항목 이전의 신청서들을 순차적으로 계산하여 초기 remaining 계산
            Double runningRemaining = vacationInfo.getAnnualVacationDays();
            for (VacationHistory previous : previousHistories) {
                if (previous.getSeq().equals(seq)) {
                    // 수정된 항목은 새로운 period로 계산
                    runningRemaining = runningRemaining - updated.getPeriod();
                } else {
                    // 기존 항목은 기존 period로 계산
                    runningRemaining = runningRemaining - previous.getPeriod();
                }
            }
            
            // 수정된 항목 이후의 신청서들을 재계산
            for (VacationHistory subsequent : subsequentHistories) {
                // 이전 신청서의 remainingVacationDays를 previousRemainingDays로 설정
                Double subPreviousRemainingDays = runningRemaining;
                Double subRemainingVacationDays = subPreviousRemainingDays - subsequent.getPeriod();
                
                subsequent.setPreviousRemainingDays(subPreviousRemainingDays);
                subsequent.setRemainingVacationDays(subRemainingVacationDays);
                subsequent.setAnnualVacationDays(vacationInfo.getAnnualVacationDays());
                
                // 다음 신청서를 위한 runningRemaining 업데이트
                runningRemaining = subRemainingVacationDays;
                
                vacationHistoryRepository.save(subsequent);
                log.info("이후 신청서 재계산 완료: seq={}, previousRemainingDays={}, remainingVacationDays={}", 
                        subsequent.getSeq(), subPreviousRemainingDays, subRemainingVacationDays);
            }
        }

        log.info("휴가 신청 수정 완료: seq={}, userId={}", seq, userId);
        return updated;
    }

    /**
     * 휴가 신청 삭제
     * 삭제 후 이후 신청서들의 remainingVacationDays를 재계산
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteVacation(Long seq, Long userId) {
        log.info("휴가 신청 삭제: seq={}, userId={}", seq, userId);

        VacationHistory vacationHistory = vacationHistoryRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 연차 내역: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT);
                });

        // 삭제 가능한 상태 확인 (null, A, RB, RC만 삭제 가능)
        String approvalStatus = vacationHistory.getApprovalStatus();
        // null이면 "A"로 간주 (초기 생성 상태)
        String actualStatus = approvalStatus == null ? "A" : approvalStatus;
        if (!"A".equals(actualStatus) && !"RB".equals(actualStatus) && !"RC".equals(actualStatus)) {
            log.warn("삭제 불가능한 상태: seq={}, approvalStatus={}", seq, approvalStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "요청 중이거나 반려된 신청만 삭제할 수 있습니다.");
        }

        UserVacationInfo vacationInfo = getUserVacationInfo(userId);

        // status에 따라 연차 차감
        String status = vacationHistory.getStatus();
        if ("R".equals(status)) {
            // 예약중이었던 경우 - 예약중 연차에서 차감
            vacationInfo.setReservedVacationDays(
                    Math.max(0, vacationInfo.getReservedVacationDays() - vacationHistory.getPeriod())
            );
        } else if ("C".equals(status)) {
            // 사용된 경우 - 사용 연차에서 차감
            vacationInfo.setUsedVacationDays(
                    Math.max(0, vacationInfo.getUsedVacationDays() - vacationHistory.getPeriod())
            );
        }

        userVacationInfoRepository.save(vacationInfo);
        
        // 삭제할 항목의 생성 시간 저장
        LocalDateTime deletedCreatedAt = vacationHistory.getCreatedAt();
        
        // 삭제 실행
        vacationHistoryRepository.delete(vacationHistory);

        // 삭제된 항목 이후에 생성된 모든 신청서들의 remainingVacationDays 재계산
        List<VacationHistory> allHistories = vacationHistoryRepository.findByUserIdOrderBySeqDesc(userId);
        List<VacationHistory> subsequentHistories = allHistories.stream()
                .filter(vh -> vh.getCreatedAt().isAfter(deletedCreatedAt))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // 생성 시간 순으로 정렬
                .toList();
        
        if (!subsequentHistories.isEmpty()) {
            log.info("삭제 후 이후 신청서 재계산 시작: userId={}, count={}", userId, subsequentHistories.size());
            
            // 삭제된 항목 이전의 모든 신청서들을 생성 시간 순으로 정렬
            List<VacationHistory> previousHistories = allHistories.stream()
                    .filter(vh -> vh.getCreatedAt().isBefore(deletedCreatedAt))
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // 생성 시간 순으로 정렬
                    .toList();
            
            // 삭제된 항목 이전의 신청서들을 순차적으로 계산하여 초기 remaining 계산
            Double runningRemaining = vacationInfo.getAnnualVacationDays();
            for (VacationHistory previous : previousHistories) {
                runningRemaining = runningRemaining - previous.getPeriod();
            }
            
            // 삭제된 항목 이후의 신청서들을 재계산
            for (VacationHistory subsequent : subsequentHistories) {
                // 이전 신청서의 remainingVacationDays를 previousRemainingDays로 설정
                Double previousRemainingDays = runningRemaining;
                Double remainingVacationDays = previousRemainingDays - subsequent.getPeriod();
                
                subsequent.setPreviousRemainingDays(previousRemainingDays);
                subsequent.setRemainingVacationDays(remainingVacationDays);
                subsequent.setAnnualVacationDays(vacationInfo.getAnnualVacationDays());
                
                // 다음 신청서를 위한 runningRemaining 업데이트
                runningRemaining = remainingVacationDays;
                
                vacationHistoryRepository.save(subsequent);
                log.info("이후 신청서 재계산 완료: seq={}, previousRemainingDays={}, remainingVacationDays={}", 
                        subsequent.getSeq(), previousRemainingDays, remainingVacationDays);
            }
        }

        log.info("휴가 신청 삭제 완료: seq={}, userId={}, status={}", seq, userId, status);
    }

    /**
     * 연차 신청서 문서 생성용 VO 생성
     *
     * @param vacationHistory 휴가 내역
     * @param user 사용자 정보
     * @param vacationInfo 연차 정보
     * @return VacationDocumentVO
     */
    public VacationDocumentVO createVacationDocumentVO(
            VacationHistory vacationHistory,
            com.vacation.api.domain.user.entity.User user,
            UserVacationInfo vacationInfo) {
        log.info("연차 신청서 문서 VO 생성: seq={}, userId={}", vacationHistory.getSeq(), vacationHistory.getUserId());
        
        String department = user.getDivision() + "/" + user.getTeam();
        
        return VacationDocumentVO.builder()
                .requestDate(vacationHistory.getRequestDate())
                .department(department)
                .applicant(user.getName())
                .startDate(vacationHistory.getStartDate())
                .endDate(vacationHistory.getEndDate())
                .vacationType(com.vacation.api.enums.VacationType.valueOf(vacationHistory.getType()))
                .reason(vacationHistory.getReason())
                .totalVacationDays(vacationInfo.getAnnualVacationDays())
                .remainingVacationDays(vacationHistory.getPreviousRemainingDays())
                .requestedVacationDays(vacationHistory.getPeriod())
                .build();
    }
}
