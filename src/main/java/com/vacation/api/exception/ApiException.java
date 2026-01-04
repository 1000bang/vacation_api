package com.vacation.api.exception;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public class ApiException extends RuntimeException {

    private final ApiErrorCode apiErrorCode;

    public ApiException(ApiErrorCode apiErrorCode) {
        super(apiErrorCode.getDescription());
        this.apiErrorCode = apiErrorCode;
    }

    public ApiException(ApiErrorCode apiErrorCode, String message) {
        super(message);
        this.apiErrorCode = apiErrorCode;
    }

    public ApiException(ApiErrorCode apiErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.apiErrorCode = apiErrorCode;
    }

    public ApiException(ApiErrorCode apiErrorCode, Throwable cause) {
        super(apiErrorCode.getDescription(), cause);
        this.apiErrorCode = apiErrorCode;
    }

    public ApiErrorCode getApiErrorCode() {
        return apiErrorCode;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return StringUtils.hasText(message) ? message : apiErrorCode.getDescription();
    }
}

