package com.vacation.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiErrorCode {
    SUCCESS("0", "요청 명령 처리 성공"),
    INVALID_REQUEST_FORMAT("900", "요청 데이터 형식이 올바르지 않습니다."),
    INVALID_VACATION_TYPE("901", "휴가 구분 값이 잘못되었습니다."),
    VALIDATION_FAILED("902", "입력값 검증에 실패했습니다."),
    DUPLICATE_EMAIL("903", "이미 존재하는 이메일입니다."),
    INVALID_LOGIN("904", "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_APPROVED("905", "승인되지 않은 사용자입니다."),
    ACCOUNT_LOCKED("913", "계정이 잠금되었습니다."),
    INVALID_REFRESH_TOKEN("906", "유효하지 않은 Refresh Token입니다."),
    USER_NOT_FOUND("907", "사용자를 찾을 수 없습니다."),
    RENTAL_SUPPORT_NOT_FOUND("908", "월세 지원 정보를 찾을 수 없습니다."),
    VACATION_INFO_NOT_FOUND("909", "사용자 연차 정보를 찾을 수 없습니다."),
    INSUFFICIENT_VACATION_DAYS("910", "잔여 연차 일수가 부족합니다."),
    VACATION_HISTORY_NOT_FOUND("911", "연차 내역을 찾을 수 없습니다."),
    CANNOT_DELETE_OLD_VACATION("912", "최신 항목만 삭제할 수 있습니다."),
    ACCESS_DENIED("913", "권한이 없습니다."),
    UNKNOWN_ERROR("9999", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String description;
}

