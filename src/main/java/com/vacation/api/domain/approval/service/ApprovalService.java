package com.vacation.api.domain.approval.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.domain.approval.entity.ApprovalRejection;
import com.vacation.api.domain.approval.repository.ApprovalRejectionRepository;
import com.vacation.api.domain.approval.response.PendingApprovalResponse;
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
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (팀장 또는 관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("팀장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                log.warn("같은 팀이 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus(ApprovalStatus.TEAM_LEADER_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal)) {
            vacationHistory.setTjApprovalId(approverId);
            vacationHistory.setTjApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            vacationHistory.setMaApprovalId(approverId);
            vacationHistory.setMaApprovalDate(LocalDate.now());
        }
        
        vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                vacationHistory.getUserId(), approverId, ApplicationType.VACATION.getCode(), seq);

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        // 권한 확인 (팀장 또는 관리자만 반려 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("팀장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                log.warn("같은 팀이 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus(ApprovalStatus.TEAM_LEADER_REJECTED.getName());
        vacationHistoryRepository.save(vacationHistory);

        // 반려 사유 저장
        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.VACATION.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.TEAM_LEADER.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                vacationHistory.getUserId(), ApplicationType.VACATION.getCode(), seq, rejectionReason, ApprovalStatus.TEAM_LEADER_REJECTED.getName());

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (본부장 또는 관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("본부장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                log.warn("같은 본부가 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal)) {
            vacationHistory.setBbApprovalId(approverId);
            vacationHistory.setBbApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            vacationHistory.setMaApprovalId(approverId);
            vacationHistory.setMaApprovalDate(LocalDate.now());
        }
        
        vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                vacationHistory.getUserId(), ApplicationType.VACATION.getCode(), seq);

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        // 권한 확인 (본부장 또는 관리자만 반려 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("본부장 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(vacationHistory.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                log.warn("같은 본부가 아님: applicant={}, approver={}", applicant.getUserId(), approverId);
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        vacationHistory.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_REJECTED.getName());
        vacationHistoryRepository.save(vacationHistory);

        // 반려 사유 저장
        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.VACATION.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                    .rejectionLevel(AuthVal.DIVISION_HEAD.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                vacationHistory.getUserId(), ApplicationType.VACATION.getCode(), seq, rejectionReason, ApprovalStatus.DIVISION_HEAD_REJECTED.getName());

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus(ApprovalStatus.TEAM_LEADER_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal)) {
            expenseClaim.setTjApprovalId(approverId);
            expenseClaim.setTjApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            expenseClaim.setMaApprovalId(approverId);
            expenseClaim.setMaApprovalDate(LocalDate.now());
        }
        
        expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                expenseClaim.getUserId(), approverId, ApplicationType.EXPENSE.getCode(), seq);

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus(ApprovalStatus.TEAM_LEADER_REJECTED.getName());
        expenseClaimRepository.save(expenseClaim);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.EXPENSE.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.TEAM_LEADER.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                expenseClaim.getUserId(), ApplicationType.EXPENSE.getCode(), seq, rejectionReason, ApprovalStatus.TEAM_LEADER_REJECTED.getName());

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal)) {
            expenseClaim.setBbApprovalId(approverId);
            expenseClaim.setBbApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            expenseClaim.setMaApprovalId(approverId);
            expenseClaim.setMaApprovalDate(LocalDate.now());
        }
        
        expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                expenseClaim.getUserId(), ApplicationType.EXPENSE.getCode(), seq);

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(expenseClaim.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        expenseClaim.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_REJECTED.getName());
        expenseClaimRepository.save(expenseClaim);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.EXPENSE.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.DIVISION_HEAD.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                expenseClaim.getUserId(), ApplicationType.EXPENSE.getCode(), seq, rejectionReason, ApprovalStatus.DIVISION_HEAD_REJECTED.getName());

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus(ApprovalStatus.TEAM_LEADER_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal)) {
            rentalSupport.setTjApprovalId(approverId);
            rentalSupport.setTjApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            rentalSupport.setMaApprovalId(approverId);
            rentalSupport.setMaApprovalDate(LocalDate.now());
        }
        
        rentalSupportRepository.save(rentalSupport);

        // 알람 생성: 신청자 및 본부장에게
        alarmService.createTeamLeaderApprovedAlarm(
                rentalSupport.getUserId(), approverId, ApplicationType.RENTAL.getCode(), seq);

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus(ApprovalStatus.TEAM_LEADER_REJECTED.getName());
        rentalSupportRepository.save(rentalSupport);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.TEAM_LEADER.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalSupport.getUserId(), ApplicationType.RENTAL.getCode(), seq, rejectionReason, ApprovalStatus.TEAM_LEADER_REJECTED.getName());

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal)) {
            rentalSupport.setBbApprovalId(approverId);
            rentalSupport.setBbApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            rentalSupport.setMaApprovalId(approverId);
            rentalSupport.setMaApprovalDate(LocalDate.now());
        }
        
        rentalSupportRepository.save(rentalSupport);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                rentalSupport.getUserId(), ApplicationType.RENTAL.getCode(), seq);

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalSupport.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        rentalSupport.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_REJECTED.getName());
        rentalSupportRepository.save(rentalSupport);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.DIVISION_HEAD.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalSupport.getUserId(), ApplicationType.RENTAL.getCode(), seq, rejectionReason, ApprovalStatus.DIVISION_HEAD_REJECTED.getName());

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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 승인할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 승인할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus(ApprovalStatus.TEAM_LEADER_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal)) {
            rentalProposal.setTjApprovalId(approverId);
            rentalProposal.setTjApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            rentalProposal.setMaApprovalId(approverId);
            rentalProposal.setMaApprovalDate(LocalDate.now());
        }
        
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
            currentStatus = ApprovalStatus.INITIAL.getName(); // null은 초기 생성 상태로 간주
        }
        if (!ApprovalStatus.INITIAL.getName().equals(currentStatus) && !ApprovalStatus.MODIFIED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.TEAM_LEADER.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "팀장만 반려할 수 있습니다.");
        }

        // 팀원 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // teamSeq로 비교 (같은 팀인지 확인)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getSeq().equals(approver.getTeamManagement().getSeq())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 팀의 신청만 반려할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus(ApprovalStatus.TEAM_LEADER_REJECTED.getName());
        rentalProposalRepository.save(rentalProposal);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.TEAM_LEADER.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq, rejectionReason, ApprovalStatus.TEAM_LEADER_REJECTED.getName());

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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 승인할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 승인할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_APPROVED.getName());
        
        // 승인자 정보 저장
        if (AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal)) {
            rentalProposal.setBbApprovalId(approverId);
            rentalProposal.setBbApprovalDate(LocalDate.now());
        } else if (AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            rentalProposal.setMaApprovalId(approverId);
            rentalProposal.setMaApprovalDate(LocalDate.now());
        }
        
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
        if (currentStatus == null || !ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(currentStatus)) {
            log.warn("반려 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려할 수 없는 상태입니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.DIVISION_HEAD.getCode().equals(approverAuthVal) && !AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "본부장만 반려할 수 있습니다.");
        }

        // 같은 본부 확인 (관리자는 제외)
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            User applicant = userRepository.findById(rentalProposal.getUserId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            // 같은 본부 확인 (division 문자열 비교)
            if (applicant.getTeamManagement() == null || approver.getTeamManagement() == null ||
                !applicant.getTeamManagement().getDivision().equals(approver.getTeamManagement().getDivision())) {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "같은 본부의 신청만 반려할 수 있습니다.");
            }
        }

        rentalProposal.setApprovalStatus(ApprovalStatus.DIVISION_HEAD_REJECTED.getName());
        rentalProposalRepository.save(rentalProposal);

        ApprovalRejection rejection = ApprovalRejection.builder()
                .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                .applicationSeq(seq)
                .rejectedBy(approverId)
                .rejectionLevel(AuthVal.DIVISION_HEAD.getName())
                .rejectionReason(rejectionReason)
                .build();
        approvalRejectionRepository.save(rejection);

        // 알람 생성: 신청자에게
        alarmService.createRejectedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq, rejectionReason, ApprovalStatus.DIVISION_HEAD_REJECTED.getName());

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
    public PendingApprovalResponse getPendingApprovals(
            Long requesterId, String type, String listType, int page, int size) {
        log.info("승인 대기 목록 조회: requesterId={}, type={}, listType={}, page={}, size={}", 
                requesterId, type, listType, page, size);

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        List<Long> userIds;
        final List<String> approvalStatuses;

        // 권한별 필터링 조건 설정
        if (AuthVal.MASTER.getCode().equals(authVal)) {
            userIds = null; // 관리자: 전체 조회
            approvalStatuses = List.of(
                    ApprovalStatus.INITIAL.getName(),
                    ApprovalStatus.MODIFIED.getName(),
                    ApprovalStatus.TEAM_LEADER_APPROVED.getName(),
                    ApprovalStatus.TEAM_LEADER_REJECTED.getName(),
                    ApprovalStatus.DIVISION_HEAD_REJECTED.getName(),
                    ApprovalStatus.DIVISION_HEAD_APPROVED.getName(),
                    ApprovalStatus.DONE.getName()); // D 상태: 최종 승인된 항목도 조회 가능
        } else if (AuthVal.DIVISION_HEAD.getCode().equals(authVal)) {
            List<User> teamMembers = userService.getUserInfoList(requesterId);
            userIds = teamMembers.stream()
                    .map(User::getUserId)
                    .toList();
            approvalStatuses = List.of(
                    ApprovalStatus.TEAM_LEADER_APPROVED.getName(), 
                    ApprovalStatus.DIVISION_HEAD_APPROVED.getName(),
                    ApprovalStatus.DONE.getName()); // D 상태: 최종 승인된 항목도 조회 가능
        } else if (AuthVal.TEAM_LEADER.getCode().equals(authVal)) {
            List<User> teamMembers = userService.getUserInfoList(requesterId);
            userIds = teamMembers.stream()
                    .map(User::getUserId)
                    .toList();
            approvalStatuses = List.of(ApprovalStatus.INITIAL.getName(), ApprovalStatus.MODIFIED.getName());
        } else {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "승인 권한이 없습니다.");
        }

        PendingApprovalResponse.PendingApprovalResponseBuilder responseBuilder = 
                PendingApprovalResponse.builder();

        // 휴가 신청 목록
        if (type == null || ApplicationType.VACATION.getCode().equals(type) || (listType != null && ApplicationType.VACATION.getLowerCase().equals(listType))) {
            responseBuilder.vacation(buildVacationList(userIds, approvalStatuses, page, size));
        }

        // 개인 비용 청구 목록
        if (type == null || ApplicationType.EXPENSE.getCode().equals(type) || (listType != null && ApplicationType.EXPENSE.getLowerCase().equals(listType))) {
            responseBuilder.expense(buildExpenseList(userIds, approvalStatuses, page, size));
        }

        // 월세 지원 신청 목록
        if (type == null || ApplicationType.RENTAL.getCode().equals(type) || (listType != null && ApplicationType.RENTAL.getLowerCase().equals(listType))) {
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
    private PendingApprovalResponse.ApplicationList buildVacationList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<VacationHistory> vacationList = getVacationList(userIds, approvalStatuses);
        
        List<PendingApprovalResponse.ApplicationItem> items = vacationList.stream()
                .map(vh -> {
                    User applicant = userRepository.findById(vh.getUserId())
                            .orElse(null);
                    return PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType(ApplicationType.VACATION.getCode())
                            .seq(vh.getSeq())
                            .userId(vh.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .startDate(vh.getStartDate())
                            .endDate(vh.getEndDate())
                            .period(vh.getPeriod())
                            .usedVacationDays(vh.getUsedVacationDays())
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
    private PendingApprovalResponse.ApplicationList buildExpenseList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<ExpenseClaim> expenseList = getExpenseList(userIds, approvalStatuses);
        
        List<PendingApprovalResponse.ApplicationItem> items = expenseList.stream()
                .map(ec -> {
                    User applicant = userRepository.findById(ec.getUserId())
                            .orElse(null);
                    return PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType(ApplicationType.EXPENSE.getCode())
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
    private PendingApprovalResponse.ApplicationList buildRentalList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<RentalSupport> rentalList = getRentalList(userIds, approvalStatuses);
        
        List<PendingApprovalResponse.ApplicationItem> items = rentalList.stream()
                .map(rs -> {
                    User applicant = userRepository.findById(rs.getUserId())
                            .orElse(null);
                    return PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType(ApplicationType.RENTAL.getCode())
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
    private PendingApprovalResponse.ApplicationList buildRentalProposalList(
            List<Long> userIds, List<String> approvalStatuses, int page, int size) {
        List<RentalProposal> rentalProposalList = getRentalProposalList(userIds, approvalStatuses);
        
        List<PendingApprovalResponse.ApplicationItem> items = rentalProposalList.stream()
                .map(rp -> {
                    User applicant = userRepository.findById(rp.getUserId())
                            .orElse(null);
                    return PendingApprovalResponse.ApplicationItem.builder()
                            .applicationType(ApplicationType.RENTAL_PROPOSAL.getCode())
                            .seq(rp.getSeq())
                            .userId(rp.getUserId())
                            .applicant(applicant != null ? applicant.getName() : "")
                            .rentalAddress(rp.getRentalAddress())
                            .contractStartDate(rp.getContractStartDate())
                            .contractEndDate(rp.getContractEndDate())
                            .contractMonthlyRent(rp.getContractMonthlyRent()) // RENTAL_PROPOSAL도 contractMonthlyRent 사용
                            .billingAmount(rp.getBillingAmount()) // RENTAL_PROPOSAL도 billingAmount 사용
                            .contractMonthlyRentProposal(rp.getContractMonthlyRent()) // 호환성을 위해 유지
                            .billingAmountProposal(rp.getBillingAmount()) // 호환성을 위해 유지
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
    private PendingApprovalResponse.ApplicationList paginate(
            List<PendingApprovalResponse.ApplicationItem> items, int page, int size) {
        long totalCount = items.size();
        int start = page * size;
        int end = Math.min(start + size, items.size());
        List<PendingApprovalResponse.ApplicationItem> pagedList = start < items.size() 
                ? items.subList(start, end) 
                : List.of();
        
        return PendingApprovalResponse.ApplicationList.builder()
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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
            if (approvalStatuses.contains(ApprovalStatus.INITIAL.getName())) {
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

    /**
     * 휴가 신청 최종 승인 (관리자)
     *
     * @param seq 휴가 신청 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveVacationByMaster(Long seq, Long approverId) {
        log.info("휴가 신청 관리자 최종 승인: seq={}, approverId={}", seq, approverId);

        VacationHistory vacationHistory = vacationHistoryRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 휴가 신청입니다.");
                });

        // 승인 가능한 상태 확인 (C만 승인 가능)
        String currentStatus = vacationHistory.getApprovalStatus();
        if (currentStatus == null || !ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("관리자 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "관리자만 최종 승인할 수 있습니다.");
        }

        vacationHistory.setApprovalStatus(ApprovalStatus.DONE.getName());
        vacationHistory.setMaApprovalId(approverId);
        vacationHistory.setMaApprovalDate(LocalDate.now());
        vacationHistoryRepository.save(vacationHistory);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                vacationHistory.getUserId(), ApplicationType.VACATION.getCode(), seq);

        log.info("휴가 신청 관리자 최종 승인 완료: seq={}", seq);
    }

    /**
     * 개인 비용 청구 최종 승인 (관리자)
     *
     * @param seq 개인 비용 청구 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveExpenseClaimByMaster(Long seq, Long approverId) {
        log.info("개인 비용 청구 관리자 최종 승인: seq={}, approverId={}", seq, approverId);

        ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 개인 비용 청구입니다.");
                });

        // 승인 가능한 상태 확인 (C만 승인 가능)
        String currentStatus = expenseClaim.getApprovalStatus();
        if (currentStatus == null || !ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("관리자 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "관리자만 최종 승인할 수 있습니다.");
        }

        expenseClaim.setApprovalStatus(ApprovalStatus.DONE.getName());
        expenseClaim.setMaApprovalId(approverId);
        expenseClaim.setMaApprovalDate(LocalDate.now());
        expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                expenseClaim.getUserId(), ApplicationType.EXPENSE.getCode(), seq);

        log.info("개인 비용 청구 관리자 최종 승인 완료: seq={}", seq);
    }

    /**
     * 월세 지원 신청 최종 승인 (관리자)
     *
     * @param seq 월세 지원 신청 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveRentalSupportByMaster(Long seq, Long approverId) {
        log.info("월세 지원 신청 관리자 최종 승인: seq={}, approverId={}", seq, approverId);

        RentalSupport rentalSupport = rentalSupportRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 지원 신청입니다.");
                });

        // 승인 가능한 상태 확인 (C만 승인 가능)
        String currentStatus = rentalSupport.getApprovalStatus();
        if (currentStatus == null || !ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("관리자 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "관리자만 최종 승인할 수 있습니다.");
        }

        rentalSupport.setApprovalStatus(ApprovalStatus.DONE.getName());
        rentalSupport.setMaApprovalId(approverId);
        rentalSupport.setMaApprovalDate(LocalDate.now());
        rentalSupportRepository.save(rentalSupport);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                rentalSupport.getUserId(), ApplicationType.RENTAL.getCode(), seq);

        log.info("월세 지원 신청 관리자 최종 승인 완료: seq={}", seq);
    }

    /**
     * 월세 품의서 최종 승인 (관리자)
     *
     * @param seq 월세 품의서 시퀀스
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveRentalProposalByMaster(Long seq, Long approverId) {
        log.info("월세 품의서 관리자 최종 승인: seq={}, approverId={}", seq, approverId);

        RentalProposal rentalProposal = rentalProposalRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 월세 품의서: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 월세 품의서입니다.");
                });

        // 승인 가능한 상태 확인 (C만 승인 가능)
        String currentStatus = rentalProposal.getApprovalStatus();
        if (currentStatus == null || !ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(currentStatus)) {
            log.warn("승인 불가능한 상태: seq={}, status={}", seq, currentStatus);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "승인할 수 없는 상태입니다.");
        }

        // 권한 확인 (관리자만 승인 가능)
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        String approverAuthVal = approver.getAuthVal();
        if (!AuthVal.MASTER.getCode().equals(approverAuthVal)) {
            log.warn("관리자 권한이 아님: approverId={}, authVal={}", approverId, approverAuthVal);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "관리자만 최종 승인할 수 있습니다.");
        }

        rentalProposal.setApprovalStatus(ApprovalStatus.DONE.getName());
        rentalProposal.setMaApprovalId(approverId);
        rentalProposal.setMaApprovalDate(LocalDate.now());
        rentalProposalRepository.save(rentalProposal);

        // 알람 생성: 신청자에게
        alarmService.createDivisionHeadApprovedAlarm(
                rentalProposal.getUserId(), ApplicationType.RENTAL_PROPOSAL.getCode(), seq);

        log.info("월세 품의서 관리자 최종 승인 완료: seq={}", seq);
    }
}
