package com.vacation.api.util;

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
     * @param authVal 사용자 권한 값 (tj: 팀장, bb: 본부장, ma: 관리자, tw: 팀원)
     * @return 초기 승인 상태
     *         - "tj" (팀장): "B" (팀장 승인 상태로 시작)
     *         - "bb" (본부장): "C" (본부장 승인 상태로 시작)
     *         - 기타: "A" (초기 상태)
     */
    public String resolveInitialApprovalStatus(String authVal) {
        if (authVal == null) {
            return "A";
        }
        
        return switch (authVal) {
            case "tj" -> {
                log.debug("팀장 권한: 초기 승인 상태를 B로 설정");
                yield "B";  // 팀장: 팀장 승인 상태로 시작
            }
            case "bb" -> {
                log.debug("본부장 권한: 초기 승인 상태를 C로 설정");
                yield "C";  // 본부장: 본부장 승인 상태로 시작
            }
            default -> {
                log.debug("일반 사용자: 초기 승인 상태를 A로 설정");
                yield "A";  // 일반 사용자: 초기 상태
            }
        };
    }
}
