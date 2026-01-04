package com.vacation.api.interceptor;

import com.vacation.api.common.TransactionIDCreator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API와 통신요청을 한 Request에 대해 시스템 인증 및 IP인증 그리고
 * API유효성을 체크를 진행하는 interceptor 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@Configuration
public class ApiInterceptor implements HandlerInterceptor {

    Logger logger = LoggerFactory.getLogger(ApiInterceptor.class);

    private final TransactionIDCreator transactionIdCreator;

    public ApiInterceptor(TransactionIDCreator transactionIdCreator) {
        this.transactionIdCreator = transactionIdCreator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String transactionId = transactionIdCreator.createTransactionId();

        logger.info("============================== start " + uri + " ==============================");
        logger.info("==================== request transaction ID generated! : " + transactionId + " =====================");

        MDC.put("transactionId", transactionId);

        // TODO: API 버전 체크, 인증 체크 등 구현 예정

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove("transactionId");
        String uri = request.getRequestURI();
        logger.info("============================== end " + uri + " ==============================");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}

