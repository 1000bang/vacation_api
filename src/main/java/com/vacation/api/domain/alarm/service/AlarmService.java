package com.vacation.api.domain.alarm.service;

import com.vacation.api.domain.alarm.entity.UserAlarm;
import com.vacation.api.domain.alarm.repository.UserAlarmRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                applicant.getDivision(), applicant.getTeam(), "tj");
        
        // 팀장이 없으면 본부장에게 알람
        if (teamLeaders.isEmpty()) {
            List<User> divisionHeads = userRepository.findByDivisionAndAuthVal(applicant.getDivision(), "bb");
            for (User divisionHead : divisionHeads) {
                UserAlarm alarm = UserAlarm.builder()
                        .userId(divisionHead.getUserId())
                        .alarmType("APPLICATION_CREATED")
                        .applicationType(applicationType)
                        .applicationSeq(applicationSeq)
                        .message(String.format("%s님이 %s 신청을 제출했습니다.", applicant.getName(), getApplicationTypeName(applicationType)))
                        .redirectUrl("/approval-list")
                        .isRead(false)
                        .build();
                userAlarmRepository.save(alarm);
                log.info("본부장 알람 생성 완료 (팀장 없음): divisionHeadId={}", divisionHead.getUserId());
            }
        }

        for (User teamLeader : teamLeaders) {
            UserAlarm alarm = UserAlarm.builder()
                    .userId(teamLeader.getUserId())
                    .alarmType("APPLICATION_CREATED")
                    .applicationType(applicationType)
                    .applicationSeq(applicationSeq)
                    .message(String.format("%s님이 %s 신청을 제출했습니다.", applicant.getName(), getApplicationTypeName(applicationType)))
                    .redirectUrl("/approval-list")
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
        String applicantMessage = "RENTAL_APPROVAL".equals(applicationType)
                ? "월세 지원 품의서 신청이 승인 되었습니다."
                : String.format("%s 신청이 팀장 승인되었습니다.", getApplicationTypeName(applicationType));
        
        UserAlarm applicantAlarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType("TEAM_LEADER_APPROVED")
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(applicantMessage)
                .redirectUrl("/my-applications")
                .isRead(false)
                .build();
        userAlarmRepository.save(applicantAlarm);
        log.info("신청자 알람 생성 완료: applicantId={}", applicantId);

        // 같은 본부의 본부장 찾기
        List<User> divisionHeads = userRepository.findByDivisionAndAuthVal(applicant.getDivision(), "bb");

        for (User divisionHead : divisionHeads) {
            UserAlarm alarm = UserAlarm.builder()
                    .userId(divisionHead.getUserId())
                    .alarmType("TEAM_LEADER_APPROVED")
                    .applicationType(applicationType)
                    .applicationSeq(applicationSeq)
                    .message(String.format("%s님의 %s 신청이 팀장 승인되어 결재 대기 중입니다.", 
                            applicant.getName(), getApplicationTypeName(applicationType)))
                    .redirectUrl("/approval-list")
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

        String message = "RENTAL_APPROVAL".equals(applicationType)
                ? "월세 지원 품의서 신청이 승인 되었습니다."
                : String.format("%s 신청이 최종 승인되었습니다.", getApplicationTypeName(applicationType));
        
        UserAlarm alarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType("DIVISION_HEAD_APPROVED")
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(message)
                .redirectUrl("/my-applications")
                .isRead(false)
                .build();
        userAlarmRepository.save(alarm);
        log.info("신청자 알람 생성 완료: applicantId={}", applicantId);
    }

    /**
     * 반려 시 신청자에게 알람 생성
     */
    @Transactional
    public void createRejectedAlarm(Long applicantId, String applicationType, Long applicationSeq, String rejectionReason) {
        log.info("반려 알람 생성: applicantId={}, applicationType={}, applicationSeq={}", 
                applicantId, applicationType, applicationSeq);

        String message = "RENTAL_APPROVAL".equals(applicationType)
                ? "월세 지원 품의서 신청이 반려 되었습니다."
                : String.format("%s 신청이 반려되었습니다. 사유: %s", 
                        getApplicationTypeName(applicationType), rejectionReason);
        
        UserAlarm alarm = UserAlarm.builder()
                .userId(applicantId)
                .alarmType("REJECTED")
                .applicationType(applicationType)
                .applicationSeq(applicationSeq)
                .message(message)
                .redirectUrl("/my-applications")
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
     */
    public List<UserAlarm> getAllAlarms(Long userId) {
        return userAlarmRepository.findByUserIdOrderByCreatedAtDesc(userId);
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
     * 신청서 타입 한글 변환
     */
    private String getApplicationTypeName(String applicationType) {
        return switch (applicationType) {
            case "VACATION" -> "휴가";
            case "EXPENSE" -> "개인 비용";
            case "RENTAL" -> "월세 지원";
            case "RENTAL_APPROVAL" -> "월세 지원 품의서";
            default -> "신청";
        };
    }
}
