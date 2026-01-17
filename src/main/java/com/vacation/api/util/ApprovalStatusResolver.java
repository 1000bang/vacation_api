package com.vacation.api.util;

import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.enums.AuthVal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 사용자 권한에 따른 승인 상태 결정 유틸리티
 * 
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Slf4j
@Component
public class ApprovalStatusResolver {

    /**
     * 사용자 권한에 따른 초기 승인 상태 결정
     * 
     * @param authVal 사용자 권한 값 (코드)
     * @return 초기 승인 상태 이름 (DB 저장용: A, AM, B, RB, C, RC)
     *         - 팀장: B
     *         - 본부장: C
     *         - 기타: A
     */
    public String resolveInitialApprovalStatus(String authVal) {
        AuthVal auth = AuthVal.fromCode(authVal);
        
        return switch (auth) {
            case TEAM_LEADER -> {
                log.debug("팀장 권한: 초기 승인 상태를 {}로 설정", ApprovalStatus.TEAM_LEADER_APPROVED.getName());
                yield ApprovalStatus.TEAM_LEADER_APPROVED.getName();
            }
            case DIVISION_HEAD -> {
                log.debug("본부장 권한: 초기 승인 상태를 {}로 설정", ApprovalStatus.DIVISION_HEAD_APPROVED.getName());
                yield ApprovalStatus.DIVISION_HEAD_APPROVED.getName();
            }
            default -> {
                log.debug("일반 사용자: 초기 승인 상태를 {}로 설정", ApprovalStatus.INITIAL.getName());
                yield ApprovalStatus.INITIAL.getName();
            }
        };
    }
}
