package com.vacation.api.domain.approval.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.domain.approval.entity.ApprovalRejection;
import com.vacation.api.domain.approval.repository.ApprovalRejectionRepository;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 승인/반려 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final VacationHistoryRepository vacationHistoryRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final RentalSupportRepository rentalSupportRepository;
    private final ApprovalRejectionRepository approvalRejectionRepository;
    private final UserRepository userRepository;
    private final AlarmService alarmService;

    /**
     * 휴가 신청 승인 (팀장)
     *
     * @param seq 휴가 신청 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveVacationByTeamLeader(Long seq, Long approverId) {
        log.info("휴가 신청 팀장 승인: seq={}, approverId={}", seq, approverId);

        VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 휴가 신청입니다.");
                });

        // 승인 가능한 상태 확인 (A 또는 AM만 승인 가능, null은 A로 간주)
        String currentStatus = vacationHistory.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (팀장 또는 관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            log.warn("팀장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                log.warn("같은 팀이 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus("B");
        vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                vacationHistory.getUserId(), approverId, "VACATION", seq);

        log.info("휴가 신청 팀장 승인 완료: seq={}", seq);
    }

    /**
     * 휴가 신청 반려 (팀장)
     *
     * @param seq 휴가 신청 시퀀스
     * @param approverId 승인자 ID
     * @param rejectionReason 반려 사유
     */
    @Transactional
    public void rejectVacationByTeamLeader(Long seq, Long approverId, String rejectionReason) {
        log.info("휴가 신청 팀장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 휴가 신청입니다.");
                });

        // 반려 가능한 상태 확인 (A 또는 AM만 반려 가능, null은 A로 간주)
        String currentStatus = vacationHistory.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        // 권한 확인 (팀장 또는 관리자만 반려 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            log.warn("팀장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                log.warn("같은 팀이 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus("RB");
        vacationHistoryRepository.save(vacationHistory);

        // 반려 사유 저장
        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("VACATION")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("TEAM_LEADER")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                vacationHistory.getUserId(), "VACATION", seq, rejectionReason);

        log.info("휴가 신청 팀장 반려 완료: seq={}", seq);
    }

    /**
     * 휴가 신청 승인 (본부장)
     *
     * @param seq 휴가 신청 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveVacationByDivisionHead(Long seq, Long approverId) {
        log.info("휴가 신청 본부장 승인: seq={}, approverId={}", seq, approverId);

        VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 휴가 신청입니다.");
                });

        // 승인 가능한 상태 확인 (B만 승인 가능)
        String currentStatus = vacationHistory.getApprovalStatus();
        // null이거나 B가 아니면 에러 (관리자는 null 체크 추가)
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (본부장 또는 관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            log.warn("본부장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                log.warn("같은 본부가 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus("C");
        vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                vacationHistory.getUserId(), "VACATION", seq);

        log.info("휴가 신청 본부장 승인 완료: seq={}", seq);
    }

    /**
     * 휴가 신청 반려 (본부장)
     *
     * @param seq 휴가 신청 시퀀스
     * @param approverId 승인자 ID
     * @param rejectionReason 반려 사유
     */
    @Transactional
    public void rejectVacationByDivisionHead(Long seq, Long approverId, String rejectionReason) {
        log.info("휴가 신청 본부장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 휴가 신청입니다.");
                });

        // 반려 가능한 상태 확인 (B만 반려 가능)
        String currentStatus = vacationHistory.getApprovalStatus();
        // null이거나 B가 아니면 에러
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        // 권한 확인 (본부장 또는 관리자만 반려 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            log.warn("본부장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                log.warn("같은 본부가 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus("RC");
        vacationHistoryRepository.save(vacationHistory);

        // 반려 사유 저장
        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("VACATION")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("DIVISION_HEAD")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                vacationHistory.getUserId(), "VACATION", seq, rejectionReason);

        log.info("휴가 신청 본부장 반려 완료: seq={}", seq);
    }

    // 개인 비용 청구 승인/반려 메서드들도 동일한 패턴으로 구현
    // (코드 중복을 줄이기 위해 나중에 리팩토링 가능)
    
    /**
     * 개인 비용 청구 승인 (팀장)
     */
    @Transactional
    public void approveExpenseClaimByTeamLeader(Long seq, Long approverId) {
        log.info("개인 비용 청구 팀장 승인: seq={}, approverId={}", seq, approverId);

        ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 개인 비용 청구입니다.");
                });

        // 승인 가능한 상태 확인 (A 또는 AM만 승인 가능, null은 A로 간주)
        String currentStatus = expenseClaim.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus("B");
        expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                expenseClaim.getUserId(), approverId, "EXPENSE", seq);

        log.info("개인 비용 청구 팀장 승인 완료: seq={}", seq);
    }

    /**
     * 개인 비용 청구 반려 (팀장)
     */
    @Transactional
    public void rejectExpenseClaimByTeamLeader(Long seq, Long approverId, String rejectionReason) {
        log.info("개인 비용 청구 팀장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 개인 비용 청구입니다.");
                });

        // 반려 가능한 상태 확인 (A 또는 AM만 반려 가능, null은 A로 간주)
        String currentStatus = expenseClaim.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus("RB");
        expenseClaimRepository.save(expenseClaim);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("EXPENSE")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("TEAM_LEADER")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                expenseClaim.getUserId(), "EXPENSE", seq, rejectionReason);

        log.info("개인 비용 청구 팀장 반려 완료: seq={}", seq);
    }

    /**
     * 개인 비용 청구 승인 (본부장)
     */
    @Transactional
    public void approveExpenseClaimByDivisionHead(Long seq, Long approverId) {
        log.info("개인 비용 청구 본부장 승인: seq={}, approverId={}", seq, approverId);

        ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 개인 비용 청구입니다.");
                });

        // 승인 가능한 상태 확인 (B만 승인 가능)
        String currentStatus = expenseClaim.getApprovalStatus();
        // null이거나 B가 아니면 에러
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus("C");
        expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                expenseClaim.getUserId(), "EXPENSE", seq);

        log.info("개인 비용 청구 본부장 승인 완료: seq={}", seq);
    }

    /**
     * 개인 비용 청구 반려 (본부장)
     */
    @Transactional
    public void rejectExpenseClaimByDivisionHead(Long seq, Long approverId, String rejectionReason) {
        log.info("개인 비용 청구 본부장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 개인 비용 청구입니다.");
                });

        // 반려 가능한 상태 확인 (B만 반려 가능)
        String currentStatus = expenseClaim.getApprovalStatus();
        // null이거나 B가 아니면 에러
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus("RC");
        expenseClaimRepository.save(expenseClaim);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("EXPENSE")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("DIVISION_HEAD")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                expenseClaim.getUserId(), "EXPENSE", seq, rejectionReason);

        log.info("개인 비용 청구 본부장 반려 완료: seq={}", seq);
    }

    /**
     * 월세 지원 신청 승인 (팀장)
     */
    @Transactional
    public void approveRentalSupportByTeamLeader(Long seq, Long approverId) {
        log.info("월세 지원 신청 팀장 승인: seq={}, approverId={}", seq, approverId);

        RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 지원 신청입니다.");
                });

        // 승인 가능한 상태 확인 (A 또는 AM만 승인 가능, null은 A로 간주)
        String currentStatus = rentalSupport.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus("B");
        rentalSupportRepository.save(rentalSupport);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                rentalSupport.getUserId(), approverId, "RENTAL", seq);

        log.info("월세 지원 신청 팀장 승인 완료: seq={}", seq);
    }

    /**
     * 월세 지원 신청 반려 (팀장)
     */
    @Transactional
    public void rejectRentalSupportByTeamLeader(Long seq, Long approverId, String rejectionReason) {
        log.info("월세 지원 신청 팀장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 지원 신청입니다.");
                });

        // 반려 가능한 상태 확인 (A 또는 AM만 반려 가능, null은 A로 간주)
        String currentStatus = rentalSupport.getApprovalStatus();
        if (currentStatus == null) {
            currentStatus = "A"; // null은 초기 생성 상태로 간주
        }
        if (!"A".equals(currentStatus) && !"AM".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"tj".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus("RB");
        rentalSupportRepository.save(rentalSupport);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("RENTAL")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("TEAM_LEADER")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalSupport.getUserId(), "RENTAL", seq, rejectionReason);

        log.info("월세 지원 신청 팀장 반려 완료: seq={}", seq);
    }

    /**
     * 월세 지원 신청 승인 (본부장)
     */
    @Transactional
    public void approveRentalSupportByDivisionHead(Long seq, Long approverId) {
        log.info("월세 지원 신청 본부장 승인: seq={}, approverId={}", seq, approverId);

        RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 지원 신청입니다.");
                });

        // 승인 가능한 상태 확인 (B만 승인 가능)
        String currentStatus = rentalSupport.getApprovalStatus();
        // null이거나 B가 아니면 에러
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus("C");
        rentalSupportRepository.save(rentalSupport);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                rentalSupport.getUserId(), "RENTAL", seq);

        log.info("월세 지원 신청 본부장 승인 완료: seq={}", seq);
    }

    /**
     * 월세 지원 신청 반려 (본부장)
     */
    @Transactional
    public void rejectRentalSupportByDivisionHead(Long seq, Long approverId, String rejectionReason) {
        log.info("월세 지원 신청 본부장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 지원 신청입니다.");
                });

        // 반려 가능한 상태 확인 (B만 반려 가능)
        String currentStatus = rentalSupport.getApprovalStatus();
        // null이거나 B가 아니면 에러
        if (currentStatus == null || !"B".equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!"bb".equals(approverAuthVal) && !"ma".equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!"ma".equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus("RC");
        rentalSupportRepository.save(rentalSupport);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType("RENTAL")
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("DIVISION_HEAD")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalSupport.getUserId(), "RENTAL", seq, rejectionReason);

        log.info("월세 지원 신청 본부장 반려 완료: seq={}", seq);
    }
}
