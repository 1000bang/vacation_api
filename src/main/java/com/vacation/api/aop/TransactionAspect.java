package com.vacation.api.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class TransactionAspect {

    @Around("execution(* com.vacation.api.domain..controller.*Controller.*(..))")
    public Object controllerExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        String transactionId = MDC.get("transactionId");
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String url = request.getRequestURL().toString();
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            log.info("transactionId: {} URL {} hit. Processing in method {}.{}", transactionId, url, className, methodName);
        } catch (Exception e) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        if (args != null)
            for (Object arg : args) {
                log.info("transactionId: {} request parameter: {}", transactionId, arg);
            }

        Object proceed = joinPoint.proceed();
        log.info("transactionId: {} response data: {}", transactionId, proceed);
        return proceed;
    }
}

