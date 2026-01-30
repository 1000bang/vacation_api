# Vacation API

Spring Boot 3.2 + Java 17로 작성된 휴가 관리 시스템 백엔드 API입니다.

## 주요 기능

- 🔐 **사용자 인증 및 권한 관리** - JWT 기반 인증, 회원가입, 로그인, 토큰 갱신
- 🏖️ **휴가 신청 및 관리** - 휴가 신청, 승인, 내역 조회, 문서 생성
- 💰 **개인비용 청구** - 개인비용 신청, 승인, 내역 조회, 문서 생성
- 🏘️ **월세 지원 신청** - 월세 지원 신청, 품의서 생성, 내역 조회
- 👥 **사용자·팀 관리** - 사용자 정보 조회/수정, 팀 관리, 승인 관리, 권한별 접근 제어
- ✍️ **서명 관리** - 서명 이미지/폰트 업로드, 미리보기, 조회·삭제
- 🔔 **알람** - 미확인 알람 조회, 전체 알람, 읽음 처리
- 📄 **문서 생성** - Word, Excel, PDF 문서 자동 생성 (동적 서명 포함)
- 🛡️ **보안** - Rate Limiting, JWT 인증, Spring Security
- 📊 **스케줄링** - 연차 상태 자동 업데이트, 7일 경과 읽은 알람 삭제

## 주요 기술 스택

### Backend Framework
- **Spring Boot 3.2.0** - 웹 애플리케이션 프레임워크
- **Spring Security** - 인증 및 권한 관리
- **Spring Data JPA** - 데이터베이스 접근
- **Spring AOP** - 관점 지향 프로그래밍 (Rate Limiting)

### Database & Cache
- **H2 Database** - 개발/테스트 환경
- **MySQL 8.0** - 프로덕션 환경
- **Redis** - Refresh Token 저장, Rate Limit, Health Check

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

---

## 프로젝트 구조 요약

- **공통**: `BaseController`(successResponse, errorResponse, createdResponse), `ApiResponse<T>`, `GlobalExceptionHandler`(ApiResponse 형식 통일)
- **도메인**: vacation, expense, rental, user, alarm, approval, attachment 등 도메인별 Controller / Service / Repository / DTO 분리
- **서명**: `SignatureService`에서 서명 업로드·조회·삭제·미리보기·폰트 목록·다운로드 처리
- **스케줄러**: `CommonScheduler`에서 연차 상태 업데이트·7일 경과 읽은 알람 삭제
- **상수**: `AuthVal`, `ApprovalStatus` 등 Enum 기반 코드/이름 상수화

---

## 주요 API 엔드포인트

### 🔐 사용자 (`/user`)

| Method | Endpoint | 설명 | Rate Limit |
|--------|----------|------|------------|
| POST | `/user/join` | 회원가입 | IP당 1시간 10회 |
| POST | `/user/login` | 로그인 | IP당 1시간 10회 |
| POST | `/user/refresh` | 토큰 갱신 | - |
| GET | `/user/info` | 내 정보 조회 | - |
| PUT | `/user/info` | 내 정보 수정 | - |
| GET | `/user/info/list` | 사용자 목록 조회 | - |
| GET | `/user/info/{userId}` | 특정 사용자 정보 조회 | - |
| PUT | `/user/info/{userId}` | 특정 사용자 정보 수정 | - |
| GET | `/user/team/list` | 본부별 팀 목록 조회 | - |

### ✍️ 서명 (`/user`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/user/signature` | 서명 생성/업로드 (이미지 또는 폰트) |
| GET | `/user/signature/preview` | 서명 미리보기 (저장 안 함) |
| GET | `/user/signature` | 서명 조회 |
| DELETE | `/user/signature` | 서명 삭제 |
| GET | `/user/signature/fonts` | 사용 가능한 폰트 목록 |
| GET | `/user/download/signature/{fileName}` | 서명 파일 다운로드 |

### 👥 팀 관리 (`/team`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/team/list` | 팀 목록 조회 |
| GET | `/team/list/{division}` | 본부별 팀 목록 조회 |
| POST | `/team` | 팀 생성 |
| PUT | `/team/{seq}` | 팀 수정 |
| DELETE | `/team/{seq}` | 팀 삭제 |
| GET | `/team/{teamSeq}/users` | 팀별 사용자 목록 |
| GET | `/team/division/{division}/users` | 본부별 사용자 목록 |

### 🔔 알람 (`/alarm`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/alarm/unread` | 미확인 알람 목록 |
| GET | `/alarm/all` | 전체 알람 목록 (읽은 것 중 3일 이내 포함) |
| PUT | `/alarm/{seq}/read` | 알람 읽음 처리 |
| PUT | `/alarm/read-all` | 모든 알람 읽음 처리 |

### ✅ 승인 (`/approval`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/approval/pending` | 승인 대기 목록 |
| POST | `/approval/vacation/{seq}/approve/team-leader` | 휴가 팀장 승인 |
| POST | `/approval/vacation/{seq}/reject/team-leader` | 휴가 팀장 반려 (RejectionRequest) |
| POST | `/approval/vacation/{seq}/approve/division-head` | 휴가 본부장 승인 |
| POST | `/approval/vacation/{seq}/reject/division-head` | 휴가 본부장 반려 |
| POST | `/approval/expense/{seq}/approve/team-leader` | 개인비용 팀장 승인/반려 |
| POST | `/approval/expense/{seq}/approve/division-head` | 개인비용 본부장 승인/반려 |
| POST | `/approval/rental/{seq}/...` | 월세 지원 승인/반려 |
| POST | `/approval/rental-proposal/{seq}/...` | 월세 품의서 승인/반려 |
| POST | `/approval/.../approve/master` | 마스터 일괄 승인 |

### 🏖️ 휴가 (`/vacation`)

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

### 🏥 Health Check

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/actuator/health` | 서비스 상태 확인 |
| GET | `/actuator/health/redis` | Redis 상태 확인 (선택) |

---

## 설치 및 실행

### 1. 사전 요구사항

- **Java 17** 이상
- **Gradle 7.x** 이상 (또는 Gradle Wrapper 사용)
- **MySQL 8.0** 이상 (프로덕션)
- **Redis** (Refresh Token, Rate Limit 등)
- **H2 Database** (개발/테스트, Gradle 의존성으로 자동 사용 가능)

### 2. 프로젝트 빌드

```bash
cd api
chmod +x gradlew   # Linux/Mac
./gradlew build    # Windows: gradlew.bat build
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
| Redis 관련 | 호스트/포트/비밀번호 | 프로파일별 설정 | ✅ (Redis 사용 시) |

---

## 주요 기능 상세

### 🔐 JWT 인증
- **Access Token**: 1시간 유효
- **Refresh Token**: Redis 저장, 7일(개발)/1일(프로덕션) 유효
- **자동 갱신**: `/user/refresh` 로 토큰 갱신

### 🛡️ Rate Limiting
- **회원가입·로그인**: IP당 1시간에 10회 제한 (`@RateLimit`)
- Bucket4j 기반 토큰 버킷 알고리즘

### 📄 문서 생성
- **Word/Excel/PDF**: Apache POI, Flying Saucer, Thymeleaf
- **동적 서명**: `FileGenerateUtil`(Spring Bean)에서 서명 이미지 삽입

### 📊 스케줄링 (CommonScheduler)
- **연차 상태 업데이트**: 매일 12시 — 종료일이 오늘인 휴가 `R` → `C`, 연차 반영
- **7일 경과 읽은 알람 삭제**: 매일 새벽 2시 — `isRead = true` 이고 7일 지난 알람 삭제

### 🔒 권한 관리
- **일반 사용자(tw)**: 본인 정보만 조회/수정
- **팀장(tj)**: 소속 팀 사용자 조회/수정
- **본부장(bb)**: 소속 본부 사용자 조회/수정
- **관리자(ma)**: 전체 조회/수정 및 승인 (AuthVal Enum 기반)

---

**버전**: v2.1.0  
**최종 업데이트**: 2026-01-26
