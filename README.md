# Vacation API

Spring Boot 3.2 + Java 17로 작성된 휴가 관리 시스템 백엔드 API입니다.

## 주요 기능

- 🔐 **사용자 인증 및 권한 관리** - JWT 기반 인증, 회원가입, 로그인, 토큰 갱신
- 🏖️ **휴가 신청 및 관리** - 휴가 신청, 승인, 내역 조회, 문서 생성
- 💰 **개인비용 청구** - 개인비용 신청, 승인, 내역 조회, 문서 생성
- 🏘️ **월세 지원 신청** - 월세 지원 신청, 품의서 생성, 내역 조회
- 👥 **사용자 관리** - 사용자 정보 조회/수정, 승인 관리, 권한별 접근 제어
- 📄 **문서 생성** - Word, Excel, PDF 문서 자동 생성
- 🛡️ **보안** - Rate Limiting, JWT 인증, Spring Security
- 📊 **스케줄링** - 휴가 상태 자동 업데이트

## 주요 기술 스택

### Backend Framework
- **Spring Boot 3.2.0** - 웹 애플리케이션 프레임워크
- **Spring Security** - 인증 및 권한 관리
- **Spring Data JPA** - 데이터베이스 접근
- **Spring AOP** - 관점 지향 프로그래밍 (Rate Limiting)

### Database
- **H2 Database** - 개발/테스트 환경
- **MySQL** - 프로덕션 환경

### Authentication & Security
- **JWT (JSON Web Token)** - 인증 토큰 관리
- **Bucket4j** - Rate Limiting

### Document Generation
- **Apache POI** - Excel 문서 생성
- **Flying Saucer** - PDF 생성
- **Thymeleaf** - HTML 템플릿 엔진

### Build Tool
- **Gradle** - 빌드 및 의존성 관리

### Language & Version
- **Java 17**
- **Lombok** - 보일러플레이트 코드 제거



## 주요 API 엔드포인트

### 🔐 사용자 인증 (`/user`)

| Method | Endpoint | 설명 | Rate Limit |
|--------|----------|------|------------|
| POST | `/user/join` | 회원가입 | IP당 1시간에 3회 |
| POST | `/user/login` | 로그인 | IP당 1시간에 5회 |
| POST | `/user/refresh` | 토큰 갱신 | - |
| GET | `/user/info` | 내 정보 조회 | - |
| PUT | `/user/info` | 내 정보 수정 | - |
| GET | `/user/info/list` | 사용자 목록 조회 | - |
| GET | `/user/info/{userId}` | 특정 사용자 정보 조회 | - |
| PUT | `/user/info/{userId}` | 특정 사용자 정보 수정 | - |

### 🏖️ 휴가 관리 (`/vacation`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/vacation/apply` | 휴가 신청 |
| GET | `/vacation/history` | 휴가 내역 조회 |
| GET | `/vacation/info` | 연차 정보 조회 |
| PUT | `/vacation/info` | 연차 정보 수정 |
| GET | `/vacation/document/{historyId}` | 휴가 신청서 다운로드 |
| DELETE | `/vacation/{historyId}` | 휴가 신청 삭제 |

### 💰 개인비용 청구 (`/expense`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/expense/claim` | 개인비용 청구 신청 |
| GET | `/expense/claim` | 개인비용 청구 목록 조회 |
| GET | `/expense/claim/{claimId}` | 개인비용 청구 상세 조회 |
| GET | `/expense/claim/{claimId}/download` | 개인비용 청구서 다운로드 |
| DELETE | `/expense/claim/{claimId}` | 개인비용 청구 삭제 |

### 🏘️ 월세 지원 (`/rental`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/rental/support` | 월세 지원 신청 |
| GET | `/rental` | 월세 지원 목록 조회 |
| GET | `/rental/{seq}` | 월세 지원 상세 조회 |
| GET | `/rental/{seq}/application` | 월세 지원 신청서 다운로드 |
| GET | `/rental/{seq}/proposal` | 월세 품의서 다운로드 |
| DELETE | `/rental/{seq}` | 월세 지원 삭제 |

### 🏥 Health Check (`/actuator`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/actuator/health` | 서비스 상태 확인 |



## 설치 및 실행

### 1. 사전 요구사항

- **Java 17** 이상
- **Gradle 7.x** 이상 (또는 Gradle Wrapper 사용)
- **MySQL 8.0** 이상 (프로덕션)
- **H2 Database** (개발/테스트, Gradle 의존성으로 자동 설치)

### 2. 프로젝트 클론 및 빌드

```bash
# 프로젝트 디렉토리로 이동
cd api

# Gradle Wrapper 권한 부여 (Linux/Mac)
chmod +x gradlew

# 의존성 설치 및 빌드
./gradlew build

# 또는 Windows
gradlew.bat build
```


### 3. 환경 변수

| 변수명 | 설명 | 기본값 | 필수 |
|--------|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `dev` | ❌ |
| `JWT_SECRET` | JWT 서명 키 (최소 32자) | - | ✅ (프로덕션) |
| `DB_USERNAME` | 데이터베이스 사용자명 | - | ✅ (프로덕션) |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | - | ✅ (프로덕션) |
| `DDL_AUTO` | JPA DDL 자동 생성 옵션 | `none` | ❌ |
| `LOG_PATH` | 로그 파일 경로 | `/data/api/logs` | ❌ |

## 주요 기능 상세

### 🔐 JWT 인증

- **Access Token**: 1시간 유효
- **Refresh Token**: 7일 유효 (개발), 1일 유효 (프로덕션)
- **자동 갱신**: `/user/refresh` 엔드포인트로 토큰 갱신 가능

### 🛡️ Rate Limiting

- **회원가입**: IP당 1시간에 3회 제한
- **로그인**: IP당 1시간에 5회 제한
- Bucket4j 기반 토큰 버킷 알고리즘 사용

### 📄 문서 생성

- **Word 문서**: Apache POI를 사용한 `.docx` 생성
- **Excel 문서**: Apache POI를 사용한 `.xlsx` 생성
- **PDF 문서**: Flying Saucer를 사용한 `.pdf` 생성
- **템플릿 기반**: Thymeleaf 템플릿 엔진 사용

### 📊 스케줄링

- **휴가 상태 자동 업데이트**: 매일 12시에 실행 (예약중 -> 사용완료)
- **7일 경과된 읽은 알람 삭제**: 매일 새벽 2시에 실행
- `@EnableScheduling` 활성화
- `CommonScheduler` 클래스에서 관리

### 🔒 권한 관리

- **일반 사용자**: 본인 정보만 조회/수정
- **팀장/본부장**: 소속 팀/본부 사용자 정보 조회/수정
- **관리자**: 모든 사용자 정보 조회/수정 및 승인



---

**버전**: v2.0.1
**최종 업데이트**: 2026-01-09
