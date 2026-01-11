package com.vacation.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 설정
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-09
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vacationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vacation Management System API")
                        .description("휴가 관리 시스템 REST API 문서\n\n" +
                                "## 인증 방법\n" +
                                "1. `/user/login` 엔드포인트로 로그인하여 `accessToken`을 받습니다.\n" +
                                "2. 우측 상단의 **Authorize** 버튼을 클릭합니다.\n" +
                                "3. `Bearer {accessToken}` 형식으로 입력합니다. (예: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`)\n" +
                                "4. **Authorize** 버튼을 클릭하여 인증을 완료합니다.\n\n" +
                                "## 권한\n" +
                                "- **ma (master)**: 관리자 - 모든 기능 접근 가능\n" +
                                "- **bb (bonbujang)**: 본부장 - 본부 내 사용자 관리 가능\n" +
                                "- **tj (teamjang)**: 팀장 - 팀 내 사용자 관리 가능\n" +
                                "- **tw (teamwon)**: 팀원 - 본인 정보만 접근 가능")
                        .version("v2.0.1")
                        .contact(new Contact()
                                .name("Knowledge Point")
                                .email("1000bang@knowledgepoint.co.kr"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://1000bang.info")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 형식: Bearer {token}")));
    }
}
