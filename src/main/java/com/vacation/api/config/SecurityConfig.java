package com.vacation.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/sample/**").permitAll()
                .requestMatchers("/user/join").permitAll()
                .requestMatchers("/user/login").permitAll()
                .requestMatchers("/user/refresh").permitAll()
                .requestMatchers("/user/info/**").permitAll()  // AOP에서 인증 체크
                .requestMatchers("/rental/**").permitAll()  // AOP에서 인증 체크
                .requestMatchers("/vacation/**").permitAll()  // AOP에서 인증 체크
                .anyRequest().permitAll()  // 기본적으로 모두 허용 (AOP에서 인증 체크)
            );
        
        return http.build();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000", 
            "http://localhost:5173",
            "http://62.171.156.211:3000",
            "https://1000bang.info",
            "https://www.1000bang.info",
            "http://1000bang.info",
            "http://www.1000bang.info"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // preflight 캐시 시간
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

