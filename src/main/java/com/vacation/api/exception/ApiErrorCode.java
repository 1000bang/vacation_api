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
    UNKNOWN_ERROR("9999", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String description;
}

