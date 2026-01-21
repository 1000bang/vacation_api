package com.vacation.api.aop;

import com.vacation.api.annotation.RateLimit;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.common.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Rate Limiting AOP
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitingService rateLimitingService;

    /**
     * @RateLimit 어노테이션이 있는 메서드에 대해 Rate Limiting 적용
     */
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        boolean allowed = rateLimitingService.tryConsume(
            request, 
            rateLimit.capacity(), 
            rateLimit.windowSeconds()
        );

        if (!allowed) {
            log.warn("Rate limit 초과: {} {}", request.getMethod(), request.getRequestURI());
            throw new ApiException(
                ApiErrorCode.INVALID_REQUEST_FORMAT, 
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            );
        }

        return joinPoint.proceed();
    }
}
