package com.vacation.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 * 프로덕션 환경에서는 Redis 필수, 개발 환경에서는 선택적
 *
 * @author vacation-api
 * @version 2.0
 * @since 2026-01-19
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.enabled:true}")
    private boolean redisEnabled;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis 연결 설정: host={}, port={}, profile={}", host, port, activeProfile);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        
        // 프로덕션 환경에서는 연결 검증 활성화
        if (isProduction()) {
            factory.setValidateConnection(true);
            log.info("프로덕션 환경: Redis 연결 검증 활성화");
        } else {
            factory.setValidateConnection(false);
            log.info("개발 환경: Redis 연결 검증 비활성화 (Redis 없이도 동작 가능)");
        }
        
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            if (isProduction()) {
                throw new IllegalStateException("프로덕션 환경에서 Redis 연결이 필수입니다. Redis를 시작하거나 spring.redis.enabled=false로 설정할 수 없습니다.");
            }
            log.warn("RedisConnectionFactory가 null입니다. Redis 없이 동작합니다.");
            return null;
        }
        
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        
        // 프로덕션 환경에서는 연결 테스트
        if (isProduction()) {
            try {
                template.getConnectionFactory().getConnection().ping();
                log.info("프로덕션 환경: Redis 연결 성공");
            } catch (Exception e) {
                log.error("프로덕션 환경에서 Redis 연결 실패: {}", e.getMessage());
                throw new IllegalStateException("프로덕션 환경에서 Redis 연결이 필수입니다.", e);
            }
        }
        
        return template;
    }

    /**
     * 애플리케이션 시작 후 Redis 연결 상태 확인
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> redisConnectionValidator() {
        return event -> {
            if (isProduction() && redisEnabled) {
                RedisTemplate<String, String> template = event.getApplicationContext()
                        .getBean(RedisTemplate.class);
                if (template == null) {
                    throw new IllegalStateException("프로덕션 환경에서 RedisTemplate이 생성되지 않았습니다.");
                }
                try {
                    template.getConnectionFactory().getConnection().ping();
                    log.info("프로덕션 환경: Redis 연결 검증 완료");
                } catch (Exception e) {
                    log.error("프로덕션 환경에서 Redis 연결 검증 실패: {}", e.getMessage());
                    throw new IllegalStateException("프로덕션 환경에서 Redis 연결이 필수입니다.", e);
                }
            }
        };
    }

    /**
     * 프로덕션 환경 여부
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }
}
