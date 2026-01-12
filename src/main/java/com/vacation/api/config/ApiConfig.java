package com.vacation.api.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vacation.api.interceptor.ApiInterceptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 프로젝트의 전체 config 구성을 정의하는 class
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Configuration
public class ApiConfig implements WebMvcConfigurer {

    @Value("${server.mode:local}")
    private String mode;

    private final ApiInterceptor apiInterceptor;

    @Autowired
    public ApiConfig(ApiInterceptor apiInterceptor) {
        this.apiInterceptor = apiInterceptor;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiInterceptor)
                .addPathPatterns("/**");
        WebMvcConfigurer.super.addInterceptors(registry);
    }

    /**
     * QueryDSL JPAQueryFactory Bean 설정
     *
     * @return JPAQueryFactory
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}

