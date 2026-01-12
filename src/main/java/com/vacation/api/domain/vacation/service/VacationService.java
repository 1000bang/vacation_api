package com.vacation.api.domain.vacation.service;

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
import com.vacation.api.vo.VacationDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
     * 연차 내역 목록 조회
     *
     * @param userId 사용자 ID
     * @return 연차 내역 목록
     */
    public List<VacationHistory> getVacationHistoryList(Long userId) {
        log.info("연차 내역 목록 조회: userId={}", userId);
        return vacationHistoryRepository.findByUserIdOrderBySeqDesc(userId);
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
                .findByDivisionAndDateRange(division, List.of("ma", "bb", "tj", "tw"), prevMonthStart, nextMonthEnd);
        
        log.info("캘린더용 휴가 목록 조회 완료: userId={}, 본부={}, 범위={}~{}, count={}", 
                userId, division, prevMonthStart, nextMonthEnd, vacationList.size());
        return vacationList;
    }

    /**
     * 연차 내역 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 연차 내역 (없으면 null)
     */
    public VacationHistory getVacationHistory(Long seq, Long userId) {
        log.info("연차 내역 조회: seq={}, userId={}", seq, userId);
        return vacationHistoryRepository.findBySeqAndUserId(seq, userId)
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
                .build();

        VacationHistory saved = vacationHistoryRepository.save(vacationHistory);

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
        Double previousRemainingDays = vacationInfo.getAnnualVacationDays() 
                - vacationInfo.getUsedVacationDays() 
                - vacationInfo.getReservedVacationDays();
        
        // 남은 연차 계산 (수정 후)
        Double remainingVacationDays = previousRemainingDays - request.getPeriod();

        // 연차 내역 수정
        vacationHistory.setStartDate(request.getStartDate());
        vacationHistory.setEndDate(request.getEndDate());
        vacationHistory.setPeriod(request.getPeriod());
        vacationHistory.setType(request.getVacationType());
        vacationHistory.setReason(request.getReason());
        vacationHistory.setRequestDate(request.getRequestDate());
        vacationHistory.setAnnualVacationDays(vacationInfo.getAnnualVacationDays());
        vacationHistory.setPreviousRemainingDays(previousRemainingDays);
        vacationHistory.setUsedVacationDays(request.getPeriod());
        vacationHistory.setRemainingVacationDays(remainingVacationDays);

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

        log.info("휴가 신청 수정 완료: seq={}, userId={}", seq, userId);
        return updated;
    }

    /**
     * 휴가 신청 삭제
     * 최신 항목(created_at desc 최상단)만 삭제 가능
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteVacation(Long seq, Long userId) {
        log.info("휴가 신청 삭제: seq={}, userId={}", seq, userId);

        // 최신 항목 조회
        VacationHistory latestHistory = vacationHistoryRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> {
                    log.warn("삭제할 연차 내역이 없음: userId={}", userId);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "삭제할 연차 내역이 없습니다.");
                });

        // 최신 항목이 아니면 삭제 불가
        if (!latestHistory.getSeq().equals(seq)) {
            log.warn("최신 항목만 삭제 가능: seq={}, latestSeq={}, userId={}", seq, latestHistory.getSeq(), userId);
            throw new ApiException(ApiErrorCode.CANNOT_DELETE_OLD_VACATION);
        }

        VacationHistory vacationHistory = vacationHistoryRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 연차 내역: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT);
                });

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
        vacationHistoryRepository.delete(vacationHistory);

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
