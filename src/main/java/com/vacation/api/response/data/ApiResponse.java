package com.vacation.api.response.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 공통 API 응답 래퍼 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String transactionId = "";
    private String resultCode = "";
    private T resultMsg;
    private String messageTemplateId;
}

