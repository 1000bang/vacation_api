package com.vacation.api.domain.approval.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.domain.approval.entity.ApprovalRejection;
import com.vacation.api.domain.approval.repository.ApprovalRejectionRepository;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
import com.vacation.api.domain.rental.entity.RentalProposal;
import com.vacation.api.domain.rental.repository.RentalProposalRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final RentalProposalRepository rentalProposalRepository;
    private final ApprovalRejectionRepository approvalRejectionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
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

    /**
     * 월세 품의서 승인 (팀장)
     */
    @Transactional
    public void approveRentalProposalByTeamLeader(Long seq, Long approverId) {
        log.info("월세 품의서 팀장 승인: seq={}, approverId={}", seq, approverId);

        RentalProposal rentalProposal = rentalProposalRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 품의서: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 품의서입니다.");
                });

        // 승인 가능한 상태 확인 (A 또는 AM만 승인 가능, null은 A로 간주)
        String currentStatus = rentalProposal.getApprovalStatus();
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
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus("B");
        rentalProposalRepository.save(rentalProposal);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                rentalProposal.getUserId(), approverId, ApplicationType.RENTAL_PROPOSAL.getCode(), seq);

        log.info("월세 품의서 팀장 승인 완료: seq={}", seq);
    }

    /**
     * 월세 품의서 반려 (팀장)
     */
    @Transactional
    public void rejectRentalProposalByTeamLeader(Long seq, Long approverId, String rejectionReason) {
        log.info("월세 품의서 팀장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        RentalProposal rentalProposal = rentalProposalRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 품의서: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 품의서입니다.");
                });

        // 반려 가능한 상태 확인 (A 또는 AM만 반려 가능, null은 A로 간주)
        String currentStatus = rentalProposal.getApprovalStatus();
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
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision()) ||
                !applicant.getTeam().equals(approver.getTeam())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus("RB");
        rentalProposalRepository.save(rentalProposal);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("TEAM_LEADER")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq, rejectionReason);

        log.info("월세 품의서 팀장 반려 완료: seq={}", seq);
    }

    /**
     * 월세 품의서 승인 (본부장)
     */
    @Transactional
    public void approveRentalProposalByDivisionHead(Long seq, Long approverId) {
        log.info("월세 품의서 본부장 승인: seq={}, approverId={}", seq, approverId);

        RentalProposal rentalProposal = rentalProposalRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 품의서: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 품의서입니다.");
                });

        // 승인 가능한 상태 확인 (B만 승인 가능)
        String currentStatus = rentalProposal.getApprovalStatus();
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
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus("C");
        rentalProposalRepository.save(rentalProposal);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq);

        log.info("월세 품의서 본부장 승인 완료: seq={}", seq);
    }

    /**
     * 월세 품의서 반려 (본부장)
     */
    @Transactional
    public void rejectRentalProposalByDivisionHead(Long seq, Long approverId, String rejectionReason) {
        log.info("월세 품의서 본부장 반려: seq={}, approverId={}, reason={}", seq, approverId, rejectionReason);

        RentalProposal rentalProposal = rentalProposalRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 품의서: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 품의서입니다.");
                });

        // 반려 가능한 상태 확인 (B만 반려 가능)
        String currentStatus = rentalProposal.getApprovalStatus();
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
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            if (!applicant.getDivision().equals(approver.getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus("RC");
        rentalProposalRepository.save(rentalProposal);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel("DIVISION_HEAD")
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq, rejectionReason);

        log.info("월세 품의서 본부장 반려 완료: seq={}", seq);
    }

    /**
     * 권한별 승인 대기 목록 조회
     * - 팀장: 해당 팀원의 A, AM
     * - 본부장: 해당 본부의 B
     * - 관리자: 전체 본부의 전체
     *
     * @param requesterId 요청자 ID
     * @param type 신청 타입 필터 (VACATION, EXPENSE, RENTAL, RENTAL_PROPOSAL, null=전체)
     * @param listType 리스트 타입 필터 (vacation, expense, rental, rental_proposal, null=전체)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 승인 대기 목록
     */
    public com.vacation.api.domain.approval.response.PendingApprovalResponse getPendingApprovals(
            Long requesterId, String type, String listType, int page, int size) {
        log.info("승인 대기 목록 조회: requesterId={}, type={}, listType={}, page={}, size={}", 
                requesterId, type, listType, page, size);

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        List<Long> userIds;
        final List<String> approvalStatuses;

        // 권한별 필터링 조건 설정
        if ("ma".equals(authVal)) {
            userIds = null; // 관리자: 전체 조회
            approvalStatuses = List.of("A", "AM", "B", "RB", "RC", "C");
        } else if ("bb".equals(authVal)) {
            List<User> teamMembers = userService.getUserInfoList(requesterId);
            userIds = teamMembers.stream()
                    .map(User::getUserId)
                    .toList();
            approvalStatuses = List.of("B", "C");
        } else if ("tj".equals(authVal)) {
            List<User> teamMembers = userService.getUserInfoList(requesterId);
            userIds = teamMembers.stream()
                    .map(User::getUserId)
                    .toList();
            approvalStatuses = List.of("A", "AM");
        } else {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "승인 권한이 없습니다.");
        }

        com.vacation.api.domain.approval.response.PendingApprovalResponse.PendingApprovalResponseBuilder responseBuilder = 
                com.vacation.api.domain.approval.response.PendingApprovalResponse.builder();

        // 휴가 신청 목록
        if (type == null || "VACATION".equals(type) || (listType != null && "vacation".equals(listType))) {
            responseBuilder.vacation(buildVacationList(userIds, approvalStatuses, page, size));
        }

        // 개인 비용 청구 목록
        if (type == null || "EXPENSE".equals(type) || (listType != null && "expense".equals(listType))) {
            responseBuilder.expense(buildExpenseList(userIds, approvalStatuses, page, size));
        }

        // 월세 지원 신청 목록
        if (type == null || "RENTAL".equals(type) || (listType != null && "rental".equals(listType))) {
            responseBuilder.rental(buildRentalList(userIds, approvalStatuses, page, size));
        }

        // 월세 품의서 목록
        if (type == null || ApplicationType.RENTAL_PROPOSAL.getCode().equals(type) || (listType != null && ApplicationType.RENTAL_PROPOSAL.getLowerCase().equals(listType))) {
            responseBuilder.rentalProposal(buildRentalProposalList(userIds, approvalStatuses, page, size));
        }

        return responseBuilder.build();
    }

    /**
     * 휴가 신청 목록 빌드
     */
    private com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList buildVacationList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<VacationHistory> vacationList = getVacationList(userIds, approvalStatuses);
        
        List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> items = vacationList.stream()
                .map(vh -> {
                    User applicant = userRepository.findById(vh.getUserId())
                            .orElse(null);
                    return com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType("VACATION")
                            .seq(vh.getSeq())
                            .userId(vh.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .startDate(vh.getStartDate())
                            .endDate(vh.getEndDate())
                            .period(vh.getPeriod())
                            .type(vh.getType())
                            .reason(vh.getReason())
                            .approvalStatus(vh.getApprovalStatus())
                            .createdAt(vh.getCreatedAt())
                            .build();
                })
                .toList();

        return paginate(items, page, size);
    }

    /**
     * 개인 비용 청구 목록 빌드
     */
    private com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList buildExpenseList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<ExpenseClaim> expenseList = getExpenseList(userIds, approvalStatuses);
        
        List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> items = expenseList.stream()
                .map(ec -> {
                    User applicant = userRepository.findById(ec.getUserId())
                            .orElse(null);
                    return com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType("EXPENSE")
                            .seq(ec.getSeq())
                            .userId(ec.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .requestDate(ec.getRequestDate())
                            .billingYyMonth(ec.getBillingYyMonth())
                            .childCnt(ec.getChildCnt())
                            .totalAmount(ec.getTotalAmount())
                            .approvalStatus(ec.getApprovalStatus())
                            .createdAt(ec.getCreatedAt())
                            .build();
                })
                .toList();

        return paginate(items, page, size);
    }

    /**
     * 월세 지원 신청 목록 빌드
     */
    private com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList buildRentalList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<RentalSupport> rentalList = getRentalList(userIds, approvalStatuses);
        
        List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> items = rentalList.stream()
                .map(rs -> {
                    User applicant = userRepository.findById(rs.getUserId())
                            .orElse(null);
                    return com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType("RENTAL")
                            .seq(rs.getSeq())
                            .userId(rs.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .requestDateRental(rs.getRequestDate())
                            .billingYyMonthRental(rs.getBillingYyMonth())
                            .contractMonthlyRent(rs.getContractMonthlyRent())
                            .billingAmount(rs.getBillingAmount())
                            .paymentDate(rs.getPaymentDate())
                            .approvalStatus(rs.getApprovalStatus())
                            .createdAt(rs.getCreatedAt())
                            .build();
                })
                .toList();

        return paginate(items, page, size);
    }

    /**
     * 월세 품의서 목록 빌드
     */
    private com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList buildRentalProposalList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<RentalProposal> rentalProposalList = getRentalProposalList(userIds, approvalStatuses);
        
        List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> items = rentalProposalList.stream()
                .map(rp -> {
                    User applicant = userRepository.findById(rp.getUserId())
                            .orElse(null);
                    return com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                            .seq(rp.getSeq())
                            .userId(rp.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .rentalAddress(rp.getRentalAddress())
                            .contractStartDate(rp.getContractStartDate())
                            .contractEndDate(rp.getContractEndDate())
                            .contractMonthlyRentProposal(rp.getContractMonthlyRent())
                            .billingAmountProposal(rp.getBillingAmount())
                            .billingStartDate(rp.getBillingStartDate())
                            .approvalStatus(rp.getApprovalStatus())
                            .createdAt(rp.getCreatedAt())
                            .build();
                })
                .toList();

        return paginate(items, page, size);
    }

    /**
     * 페이징 처리
     */
    private com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList paginate(
            List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> items, int page, int size) {
        long totalCount = items.size();
        int start = page * size;
        int end = Math.min(start + size, items.size());
        List<com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationItem> pagedList = start < items.size() 
                ? items.subList(start, end) 
                : List.of();
        
        return com.vacation.api.domain.approval.response.PendingApprovalResponse.ApplicationList.builder()
                .list(pagedList)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 휴가 신청 목록 조회 (공통 로직)
     */
    private List<VacationHistory> getVacationList(List<Long> userIds, List<String> approvalStatuses) {
        List<VacationHistory> vacationList;
        if (userIds == null) {
            List<VacationHistory> filteredList = vacationHistoryRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<VacationHistory> nullStatusList = vacationHistoryRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(VacationHistory::getSeq)
                        .collect(Collectors.toSet());
                List<VacationHistory> additionalList = nullStatusList.stream()
                        .filter(vh -> !existingSeqs.contains(vh.getSeq()))
                        .toList();
                vacationList = new java.util.ArrayList<>(filteredList);
                vacationList.addAll(additionalList);
                vacationList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                vacationList = filteredList;
            }
        } else {
            List<VacationHistory> filteredList = vacationHistoryRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                    userIds, approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<VacationHistory> nullStatusList = vacationHistoryRepository.findByUserIdInOrderByStartDateAsc(userIds).stream()
                        .filter(vh -> vh.getApprovalStatus() == null)
                        .toList();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(VacationHistory::getSeq)
                        .collect(Collectors.toSet());
                List<VacationHistory> additionalList = nullStatusList.stream()
                        .filter(vh -> !existingSeqs.contains(vh.getSeq()))
                        .toList();
                vacationList = new java.util.ArrayList<>(filteredList);
                vacationList.addAll(additionalList);
                vacationList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                vacationList = filteredList;
            }
        }
        return vacationList;
    }

    /**
     * 개인 비용 청구 목록 조회 (공통 로직)
     */
    private List<ExpenseClaim> getExpenseList(List<Long> userIds, List<String> approvalStatuses) {
        List<ExpenseClaim> expenseList;
        if (userIds == null) {
            List<ExpenseClaim> filteredList = expenseClaimRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<ExpenseClaim> nullStatusList = expenseClaimRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(ExpenseClaim::getSeq)
                        .collect(Collectors.toSet());
                List<ExpenseClaim> additionalList = nullStatusList.stream()
                        .filter(ec -> !existingSeqs.contains(ec.getSeq()))
                        .toList();
                expenseList = new java.util.ArrayList<>(filteredList);
                expenseList.addAll(additionalList);
                expenseList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                expenseList = filteredList;
            }
        } else {
            List<ExpenseClaim> filteredList = expenseClaimRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                    userIds, approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<ExpenseClaim> nullStatusList = expenseClaimRepository.findByUserIdOrderBySeqDesc(userIds.get(0)).stream()
                        .filter(ec -> {
                            if (ec.getApprovalStatus() == null) {
                                return userIds.contains(ec.getUserId());
                            }
                            return false;
                        })
                        .toList();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(ExpenseClaim::getSeq)
                        .collect(Collectors.toSet());
                List<ExpenseClaim> additionalList = nullStatusList.stream()
                        .filter(ec -> !existingSeqs.contains(ec.getSeq()))
                        .toList();
                expenseList = new java.util.ArrayList<>(filteredList);
                expenseList.addAll(additionalList);
                expenseList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                expenseList = filteredList;
            }
        }
        return expenseList;
    }

    /**
     * 월세 지원 신청 목록 조회 (공통 로직)
     */
    private List<RentalSupport> getRentalList(List<Long> userIds, List<String> approvalStatuses) {
        List<RentalSupport> rentalList;
        if (userIds == null) {
            List<RentalSupport> filteredList = rentalSupportRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<RentalSupport> nullStatusList = rentalSupportRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(RentalSupport::getSeq)
                        .collect(Collectors.toSet());
                List<RentalSupport> additionalList = nullStatusList.stream()
                        .filter(rs -> !existingSeqs.contains(rs.getSeq()))
                        .toList();
                rentalList = new java.util.ArrayList<>(filteredList);
                rentalList.addAll(additionalList);
                rentalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                rentalList = filteredList;
            }
        } else {
            List<RentalSupport> filteredList = rentalSupportRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                    userIds, approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<RentalSupport> nullStatusList = rentalSupportRepository.findByUserIdOrderBySeqDesc(userIds.get(0)).stream()
                        .filter(rs -> {
                            if (rs.getApprovalStatus() == null) {
                                return userIds.contains(rs.getUserId());
                            }
                            return false;
                        })
                        .toList();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(RentalSupport::getSeq)
                        .collect(Collectors.toSet());
                List<RentalSupport> additionalList = nullStatusList.stream()
                        .filter(rs -> !existingSeqs.contains(rs.getSeq()))
                        .toList();
                rentalList = new java.util.ArrayList<>(filteredList);
                rentalList.addAll(additionalList);
                rentalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                rentalList = filteredList;
            }
        }
        return rentalList;
    }

    /**
     * 월세 품의서 목록 조회 (공통 로직)
     */
    private List<RentalProposal> getRentalProposalList(List<Long> userIds, List<String> approvalStatuses) {
        List<RentalProposal> rentalProposalList;
        if (userIds == null) {
            List<RentalProposal> filteredList = rentalProposalRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<RentalProposal> nullStatusList = rentalProposalRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(RentalProposal::getSeq)
                        .collect(Collectors.toSet());
                List<RentalProposal> additionalList = nullStatusList.stream()
                        .filter(rp -> !existingSeqs.contains(rp.getSeq()))
                        .toList();
                rentalProposalList = new java.util.ArrayList<>(filteredList);
                rentalProposalList.addAll(additionalList);
                rentalProposalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                rentalProposalList = filteredList;
            }
        } else {
            List<RentalProposal> filteredList = rentalProposalRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                    userIds, approvalStatuses);
            if (approvalStatuses.contains("A")) {
                List<RentalProposal> nullStatusList = rentalProposalRepository.findAll().stream()
                        .filter(rp -> {
                            if (rp.getApprovalStatus() == null) {
                                return userIds.contains(rp.getUserId());
                            }
                            return false;
                        })
                        .toList();
                Set<Long> existingSeqs = filteredList.stream()
                        .map(RentalProposal::getSeq)
                        .collect(Collectors.toSet());
                List<RentalProposal> additionalList = nullStatusList.stream()
                        .filter(rp -> !existingSeqs.contains(rp.getSeq()))
                        .toList();
                rentalProposalList = new java.util.ArrayList<>(filteredList);
                rentalProposalList.addAll(additionalList);
                rentalProposalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                rentalProposalList = filteredList;
            }
        }
        return rentalProposalList;
    }
}
