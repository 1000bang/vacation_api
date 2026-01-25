package com.vacation.api.domain.alarm.service;

import com.vacation.api.domain.alarm.entity.UserAlarm;
import com.vacation.api.domain.alarm.repository.UserAlarmRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.enums.RedirectUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vacation.api.domain.alarm.response.AlarmResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 알람 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;

    /**
     * 신청서 작성 시 팀장에게 알람 생성
     */
    @Transactional
    public void createApplicationCreatedAlarm(Long applicantId, String applicationType, Long applicationSeq) {
        log.info("신청서 작성 알람 생성: applicantId={}, applicationType={}, applicationSeq={}", 
                applicantId, applicationType, applicationSeq);

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("신청자를 찾을 수 없습니다."));

        // 같은 팀의 팀장 찾기
        List<User> teamLeaders = userRepository.findByDivisionAndTeamAndAuthVal(
                applicant.getDivision(), applicant.getTeam(), AuthVal.TEAM_LEADER.getCode());
        
        // 팀장이 없으면 본부장에게 알람
        if (teamLeaders.isEmpty()) {
            List<User> divisionHeads = userRepository.findByDivisionAndAuthVal(applicant.getDivision(), AuthVal.DIVISION_HEAD.getCode());
            for (User divisionHead : divisionHeads) {
                UserAlarm alarm = UserAlarm.builder()
                        .userId(divisionHead.getUserId())
                        .alarmType(ApprovalStatus.INITIAL.getCode())
                        .applicationType(applicationType)
                        .applicationSeq(applicationSeq)
                        .message(String.format("%s님이 %s 신청을 제출했습니다.", applicant.getName(), getApplicationTypeName(applicationType)))
                        .redirectUrl(RedirectUrl.APPROVAL_LIST.getCode())
                        .isRead(false)
                        .build();
                userAlarmRepository.save(alarm);
                log.info("본부장 알람 생성 완료 (팀장 없음): divisionHeadId={}", divisionHead.getUserId());
            }
        }

        for (User teamLeader : teamLeaders) {
            UserAlarm alarm = UserAlarm.builder()
                    .userId(teamLeader.getUserId())
                    .alarmType(ApprovalStatus.INITIAL.getCode())
                    .applicationType(applicationType)
                    .applicationSeq(applicationSeq)
                    .message(String.format("%s님이 %s 신청을 제출했습니다.", applicant.getName(), getApplicationTypeName(applicationType)))
                    .redirectUrl(RedirectUrl.APPROVAL_LIST.getCode())
                    .isRead(false)
                    .build();
            userAlarmRepository.save(alarm);
            log.info("팀장 알람 생성 완료: teamLeaderId={}", teamLeader.getUserId());
        }
    }

    /**
     * 팀장 승인 시 신청자 및 본부장에게 알람 생성
     */
    @Transactional
    public void createTeamLeaderApprovedAlarm(Long applicantId, Long approverId, String applicationType, Long applicationSeq) {
        log.info("팀장 승인 알람 생성: applicantId={}, approverId={}, applicationType={}, applicationSeq={}", 
                applicantId, approverId, applicationType, applicationSeq);

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("신청자를 찾을 수 없습니다."));

        // 신청자에게 알람
        String applicantMessage = ApplicationType.RENTAL_PROPOSAL.getCode().equals(applicationType)
                ? "월세 품의서 신청이 승인 되었습니다."
                : String.format("%s 신청이 팀장 승인되었습니다.", getApplicationTypeName(applicationType));
        
        UserAlarm applicantAlarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType(ApprovalStatus.TEAM_LEADER_APPROVED.getCode())
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(applicantMessage)
                .redirectUrl(RedirectUrl.MY_APPLICATIONS.getCode())
                .isRead(false)
                .build();
        userAlarmRepository.save(applicantAlarm);
        log.info("신청자 알람 생성 완료: applicantId={}", applicantId);

        // 같은 본부의 본부장 찾기
        List<User> divisionHeads = userRepository.findByDivisionAndAuthVal(applicant.getDivision(), AuthVal.DIVISION_HEAD.getCode());

        for (User divisionHead : divisionHeads) {
            UserAlarm alarm = UserAlarm.builder()
                    .userId(divisionHead.getUserId())
                    .alarmType(ApprovalStatus.TEAM_LEADER_APPROVED.getCode())
                    .applicationType(applicationType)
                    .applicationSeq(applicationSeq)
                    .message(String.format("%s님의 %s 신청이 팀장 승인되어 결재 대기 중입니다.", 
                            applicant.getName(), getApplicationTypeName(applicationType)))
                    .redirectUrl(RedirectUrl.APPROVAL_LIST.getCode())
                    .isRead(false)
                    .build();
            userAlarmRepository.save(alarm);
            log.info("본부장 알람 생성 완료: divisionHeadId={}", divisionHead.getUserId());
        }
    }

    /**
     * 본부장 승인 시 신청자에게 알람 생성
     */
    @Transactional
    public void createDivisionHeadApprovedAlarm(Long applicantId, String applicationType, Long applicationSeq) {
        log.info("본부장 승인 알람 생성: applicantId={}, applicationType={}, applicationSeq={}", 
                applicantId, applicationType, applicationSeq);

        String message = ApplicationType.RENTAL_PROPOSAL.getCode().equals(applicationType)
                ? "월세 품의서 신청이 승인 되었습니다."
                : String.format("%s 신청이 최종 승인되었습니다.", getApplicationTypeName(applicationType));
        
        UserAlarm alarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType(ApprovalStatus.DIVISION_HEAD_APPROVED.getCode())
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(message)
                .redirectUrl(RedirectUrl.MY_APPLICATIONS.getCode())
                .isRead(false)
                .build();
        userAlarmRepository.save(alarm);
        log.info("신청자 알람 생성 완료: applicantId={}", applicantId);
    }

    /**
     * 반려 시 신청자에게 알람 생성
     * 
     * @param applicantId 신청자 ID
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @param rejectionReason 반려 사유
     * @param rejectionStatus 반려 상태 (RB: 팀장 반려, RC: 본부장 반려)
     */
    @Transactional
    public void createRejectedAlarm(Long applicantId, String applicationType, Long applicationSeq, String rejectionReason, String rejectionStatus) {
        log.info("반려 알람 생성: applicantId={}, applicationType={}, applicationSeq={}, rejectionStatus={}", 
                applicantId, applicationType, applicationSeq, rejectionStatus);

        String message = ApplicationType.RENTAL_PROPOSAL.getCode().equals(applicationType)
                ? "월세 품의서 신청이 반려 되었습니다."
                : String.format("%s 신청이 반려되었습니다. 사유: %s", 
                        getApplicationTypeName(applicationType), rejectionReason);
        
        // 반려 상태에 따라 알람 타입 결정
        ApprovalStatus alarmStatus;
        if (ApprovalStatus.TEAM_LEADER_REJECTED.getName().equals(rejectionStatus)) {
            alarmStatus = ApprovalStatus.TEAM_LEADER_REJECTED;
        } else if (ApprovalStatus.DIVISION_HEAD_REJECTED.getName().equals(rejectionStatus)) {
            alarmStatus = ApprovalStatus.DIVISION_HEAD_REJECTED;
        } else {
            // 기본값: 팀장 반려로 간주
            log.warn("알 수 없는 반려 상태: {}, 팀장 반려로 처리", rejectionStatus);
            alarmStatus = ApprovalStatus.TEAM_LEADER_REJECTED;
        }
        
        UserAlarm alarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType(alarmStatus.getCode())
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(message)
                .redirectUrl(RedirectUrl.MY_APPLICATIONS.getCode())
                .isRead(false)
                .build();
        userAlarmRepository.save(alarm);
        log.info("신청자 알람 생성 완료: applicantId={}", applicantId);
    }

    /**
     * 사용자의 읽지 않은 알람 목록 조회
     */
    public List<UserAlarm> getUnreadAlarms(Long userId) {
        return userAlarmRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자의 모든 알람 목록 조회
     * 읽은 알람 중 3일이 지난 것은 제외
     */
    public List<UserAlarm> getAllAlarms(Long userId) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        return userAlarmRepository.findByUserIdExcludingOldReadAlarms(userId, threeDaysAgo);
    }

    /**
     * UserAlarm을 AlarmResponse로 변환 (redirectUrl 코드를 실제 URL로 변환)
     */
    public AlarmResponse toAlarmResponse(UserAlarm alarm) {
        String redirectUrl = null;
        if (alarm.getRedirectUrl() != null) {
            try {
                redirectUrl = RedirectUrl.fromCode(alarm.getRedirectUrl()).getUrl();
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 redirectUrl 코드: {}", alarm.getRedirectUrl());
                redirectUrl = alarm.getRedirectUrl(); // 코드를 그대로 반환
            }
        }
        
        // alarmType 코드를 실제 값으로 변환
        String alarmTypeName = null;
        if (alarm.getAlarmType() != null) {
            try {
                ApprovalStatus status = ApprovalStatus.fromCodeOrName(alarm.getAlarmType());
                alarmTypeName = status.getName();
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 alarmType 코드: {}", alarm.getAlarmType());
                alarmTypeName = alarm.getAlarmType(); // 변환 실패 시 원본 반환
            }
        }
        
        return AlarmResponse.builder()
                .seq(alarm.getSeq())
                .userId(alarm.getUserId())
                .alarmType(alarmTypeName)
                .applicationType(alarm.getApplicationType())
                .applicationSeq(alarm.getApplicationSeq())
                .message(alarm.getMessage())
                .isRead(alarm.getIsRead())
                .redirectUrl(redirectUrl)
                .createdAt(alarm.getCreatedAt())
                .build();
    }

    /**
     * 알람 읽음 처리
     */
    @Transactional
    public void markAsRead(Long alarmSeq) {
        userAlarmRepository.markAsRead(alarmSeq);
    }

    /**
     * 사용자의 모든 알람 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        userAlarmRepository.markAllAsRead(userId);
    }

    /**
     * 신청서 타입 한글 변환 (ApplicationType enum 사용)
     */
    private String getApplicationTypeName(String applicationType) {
        if (applicationType == null) {
            return "신청";
        }
        try {
            ApplicationType type = ApplicationType.fromCode(applicationType);
            return type.getDescription();
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 신청 타입: {}", applicationType);
            return "신청";
        }
    }
}
