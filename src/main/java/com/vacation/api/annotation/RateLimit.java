package com.vacation.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting을 적용할 메서드를 표시하는 어노테이션
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 시간당 허용 요청 수
     */
    int capacity() default 5;
    
    /**
     * 시간 윈도우 (초)
     */
    int windowSeconds() default 3600; // 기본 1시간
}
