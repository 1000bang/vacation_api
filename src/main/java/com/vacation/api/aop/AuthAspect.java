package com.vacation.api.aop;

import com.vacation.api.annotation.RequiresAuth;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.util.AuthUtil;
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
 * 인증 체크 AOP
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final AuthUtil authUtil;

    /**
     * @RequiresAuth 어노테이션이 있는 메서드에 대해 인증 체크
     *
     * @param joinPoint 조인 포인트
     * @param requiresAuth RequiresAuth 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 예외
     */
    @Around("@annotation(requiresAuth)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, RequiresAuth requiresAuth) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        HttpServletRequest request = attributes.getRequest();
        Long userId = authUtil.getUserIdFromRequest(request);

        if (userId == null) {
            log.warn("인증이 필요합니다.");
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        // userId를 메서드 파라미터로 전달하기 위해 request에 저장
        request.setAttribute("userId", userId);

        return joinPoint.proceed();
    }
}

