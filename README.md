# Portfolio Backend — Production-Grade Spring Boot

> **GitHub**: https://github.com/kikjs75/oauth2_based_project

A production-ready backend portfolio demonstrating:
- OAuth2 JWT Assertion client (Google/Microsoft)
- FCM HTTP v1 push notifications
- JWT-based authentication with role escalation
- Multi-module Gradle architecture
- DB-agnostic persistence (MariaDB/PostgreSQL + Flyway)
- Observability: Micrometer, Prometheus, Grafana, Jaeger (OTLP)

## Project Structure

```
oauth2_based_project/
├── app/                                   ← 메인 Spring Boot 애플리케이션
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/portfolio/app/
│       │   │   ├── PortfolioApplication.java
│       │   │   ├── admin/                         ← ADMIN: WRITER 역할 부여
│       │   │   │   ├── AdminController.java
│       │   │   │   └── AdminService.java
│       │   │   ├── auth/                          ← 회원가입 / 로그인 / JWT 발급
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── AuthService.java
│       │   │   │   └── dto/
│       │   │   │       ├── LoginRequest.java
│       │   │   │       ├── SignupRequest.java
│       │   │   │       └── TokenResponse.java
│       │   │   ├── common/                        ← 공통 에러 응답 / 예외 핸들러
│       │   │   │   ├── ErrorResponse.java
│       │   │   │   └── GlobalExceptionHandler.java
│       │   │   ├── config/                        ← FcmClient 빈 설정
│       │   │   │   └── FcmClientConfig.java
│       │   │   ├── post/                          ← 게시글 CRUD (WRITER/ADMIN 전용)
│       │   │   │   ├── Post.java
│       │   │   │   ├── PostController.java
│       │   │   │   ├── PostRepository.java
│       │   │   │   ├── PostService.java
│       │   │   │   └── dto/
│       │   │   │       ├── CreatePostRequest.java
│       │   │   │       └── PostResponse.java
│       │   │   ├── push/                          ← FCM 테스트 푸시 (WRITER/ADMIN)
│       │   │   │   ├── PushController.java
│       │   │   │   ├── PushService.java
│       │   │   │   └── dto/
│       │   │   │       └── PushRequest.java
│       │   │   ├── security/                      ← JWT 필터 / Spring Security 설정
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── JwtProperties.java
│       │   │   │   ├── JwtTokenProvider.java
│       │   │   │   └── SecurityConfig.java
│       │   │   └── user/                          ← User 엔티티 / Repository
│       │   │       ├── User.java
│       │   │       └── UserRepository.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/                  ← Flyway {vendor} 자동 분기
│       │           ├── mariadb/V1__init_schema.sql
│       │           └── postgresql/V1__init_schema.sql
│       └── test/
│           ├── java/com/portfolio/app/
│           │   ├── auth/
│           │   │   ├── AuthIntegrationTest.java        ← MariaDB 통합 테스트 (signup/login)
│           │   │   └── AuthPostgresIntegrationTest.java← PostgreSQL 통합 테스트 (signup/login)
│           │   └── support/
│           │       ├── MariaDbContainerSupport.java    ← GenericContainer + log wait + 브릿지 IP
│           │       └── PostgresContainerSupport.java   ← GenericContainer + log wait + 브릿지 IP
│           └── resources/
│               ├── application-test.yml                ← MariaDB 테스트 프로파일
│               └── application-test-pg.yml             ← PostgreSQL 테스트 프로파일
│
├── oauth2-awt-core/                       ← OAuth2 JWT Assertion 라이브러리 (재사용 가능)
│   ├── build.gradle
│   └── src/main/java/com/portfolio/oauth2/awt/core/
│       ├── AssertionConfig.java           ← Google/Microsoft 설정 (Builder 패턴)
│       ├── AssertionTokenClient.java      ← 공개 API 진입점
│       ├── CachedTokenProvider.java       ← Caffeine 캐시 + ReentrantLock single-flight
│       ├── JwtAssertionBuilder.java       ← RS256 JWT 서명 (Nimbus JOSE)
│       ├── TokenEndpointClient.java       ← 토큰 엔드포인트 HTTP 호출 (RestClient)
│       └── TokenResponse.java             ← 액세스 토큰 + 만료시각, isExpiredWithSkew() 헬퍼
│   └── src/test/java/com/portfolio/oauth2/awt/core/
│       ├── CachedTokenProviderTest.java   ← Mockito: 캐시 히트 시 단 1회만 fetch 검증
│       └── JwtAssertionBuilderTest.java   ← 인메모리 RSA 키로 JWT 서명/파싱 검증
│
├── oauth2-awt-starter/                    ← Spring Boot Auto-configuration
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/portfolio/oauth2/awt/starter/
│       │   ├── Oauth2AwtAutoConfiguration.java  ← @ConditionalOnExpression 조건부 빈 등록
│       │   └── Oauth2AwtProperties.java         ← oauth2.awt.* 설정 바인딩
│       └── resources/META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  ← Spring Boot 자동 구성 등록 파일
│
├── fcm-client/                            ← FCM HTTP v1 클라이언트
│   ├── build.gradle
│   └── src/main/java/com/portfolio/fcm/
│       ├── FcmClient.java                 ← Bearer 토큰 획득 후 FCM API 호출, Micrometer Timer
│       ├── FcmConfig.java                 ← fcm.* 설정 바인딩
│       └── FcmMessage.java                ← FCM HTTP v1 메시지 페이로드
│
├── monitoring/                            ← 모니터링 스택 설정
│   ├── prometheus/prometheus.yml          ← /actuator/prometheus 스크레이핑
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml     ← Prometheus 데이터소스 자동 프로비저닝
│       └── dashboards/dashboard.yml       ← 대시보드 프로바이더 설정
│
├── build.gradle          ← 루트 공통 의존성 / BOM 관리
├── settings.gradle       ← 멀티모듈 서브프로젝트 선언
├── docker-compose.yml    ← MariaDB, PostgreSQL, Prometheus, Grafana, Jaeger, App 전체 스택
├── Dockerfile            ← 멀티스테이지 빌드 (Gradle → JRE 17 최소 이미지)
├── gradlew               ← Gradle Wrapper 실행 스크립트
└── .env.example          ← 환경변수 템플릿 (실제 secrets 미포함)
```

## Modules

| Module | Description |
|---|---|
| `app` | Main Spring Boot application |
| `oauth2-awt-core` | OAuth2 JWT Bearer assertion library |
| `oauth2-awt-starter` | Spring Boot auto-configuration for oauth2-awt-core |
| `fcm-client` | FCM HTTP v1 API client |

## Implementation

| 항목 | 내용 |
|---|---|
| **멀티모듈** | `app`, `oauth2-awt-core`, `oauth2-awt-starter`, `fcm-client` |
| **JWT 인증** | Nimbus JOSE HS256, signup/login, role-based 접근 제어 |
| **역할 관리** | USER → WRITER 승격 (ADMIN only), 게시판 WRITER/ADMIN 전용 |
| **OAuth2 AWT** | RS256 JWT assertion, Caffeine 캐시, single-flight, retry |
| **FCM** | HTTP v1 API, Micrometer Timer 메트릭 |
| **DB** | MariaDB(기본)/PostgreSQL(옵션), Flyway `{vendor}` 자동 분기, DB-agnostic |
| **모니터링** | Prometheus + Grafana + Jaeger (docker-compose) |
| **보안** | 실 credentials 미포함, 환경변수/파일 경로만 사용 |
| **테스트** | Testcontainers 통합테스트 (MariaDB/PostgreSQL), Mockito 단위테스트 |

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose

### Run with Docker Compose
```bash
# Copy and configure environment
cp .env.example .env
# Edit .env with your values:
#   JWT_SECRET, FCM_PROJECT_ID
#   GOOGLE_SERVICE_ACCOUNT_KEY_PATH=/host/path/to/service-account.json

# Start all services (MariaDB + Prometheus + Grafana + Jaeger + App)
docker-compose up -d
```

> **FCM 설정 흐름**
> `.env`의 `GOOGLE_SERVICE_ACCOUNT_KEY_PATH`(호스트 경로)가 docker-compose volume으로
> 컨테이너 내부 `/secrets/google-service-account.json`에 자동 마운트됩니다.
> 미설정 시 FCM 빈이 비활성화되어 앱은 정상 기동되며, `/api/push/test`만 비활성 상태가 됩니다.

### Run locally (development)
```bash
# Start dependencies only
docker-compose up -d mariadb jaeger

# Run the app
./gradlew :app:bootRun
```

## API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | /api/auth/signup | Public | Register a new user |
| POST | /api/auth/login | Public | Obtain JWT token |
| POST | /api/admin/users/{id}/grant-writer | ADMIN | Promote user to WRITER |
| POST | /api/posts | WRITER, ADMIN | Create a post |
| GET | /api/posts | Authenticated | List all posts |
| GET | /api/posts/{id} | Authenticated | Get a post |
| POST | /api/push/test | WRITER, ADMIN | Send test FCM push |

## Monitoring

| Service | URL |
|---|---|
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Jaeger | http://localhost:16686 |

## Configuration

All secrets are via environment variables (see `.env.example`):

```
JWT_SECRET=...
DB_URL=jdbc:mariadb://localhost:3306/portfolio
DB_USERNAME=portfolio
DB_PASSWORD=portfolio

# Google OAuth2 AWT + FCM
# GOOGLE_SERVICE_ACCOUNT_KEY_PATH: 호스트 파일 경로 → docker-compose가 컨테이너 내부로 마운트
GOOGLE_SERVICE_ACCOUNT_KEY_PATH=/path/to/service-account.json
FCM_PROJECT_ID=my-firebase-project

# Microsoft OAuth2 AWT (선택)
MICROSOFT_CLIENT_ID=your-azure-app-client-id
MICROSOFT_TENANT_ID=your-azure-tenant-id
MICROSOFT_PRIVATE_KEY_PEM_PATH=/path/to/client-private-key.pem
MICROSOFT_KEY_ID=your-certificate-thumbprint
```

| 항목 | 로컬 개발 (`bootRun`) | Docker Compose |
|---|---|---|
| `GOOGLE_SERVICE_ACCOUNT_KEY_PATH` | 호스트 경로 직접 참조 | 호스트 경로 → `/secrets/google-service-account.json` 자동 마운트 |
| FCM 미설정 시 | 앱 기동 O, push 엔드포인트만 비활성 | 동일 |
| Microsoft | 선택 사항, 미설정 시 빈 비활성 | 동일 |

## Security

- No real credentials are committed to the repository
- Google service account JSON is loaded via file path only
- Microsoft private key is loaded via PEM file path only
- JWT secret via environment variable only

## Database Support

Default: **MariaDB**

Flyway 마이그레이션은 `{vendor}` 플레이스홀더로 DB별 스크립트를 자동 선택합니다:

```
db/migration/
├── mariadb/
│   └── V1__init_schema.sql   ← AUTO_INCREMENT, DATETIME(6)
└── postgresql/
    └── V1__init_schema.sql   ← GENERATED ALWAYS AS IDENTITY, TIMESTAMP(6)
```

Switch to PostgreSQL:
```yaml
# application.yml overrides
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/portfolio
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Or use the postgres Docker Compose profile:
```bash
docker-compose --profile postgres up -d postgres
```

## Testing

통합 테스트는 Testcontainers로 실제 DB 컨테이너를 기동합니다. CI에 별도 DB 설치 불필요.

| 항목 | MariaDB 테스트 | PostgreSQL 테스트 |
|---|---|---|
| 베이스 클래스 | `MariaDbContainerSupport` | `PostgresContainerSupport` |
| 프로파일 | `test` | `test-pg` |
| yml | `application-test.yml` | `application-test-pg.yml` |
| dialect | `MariaDBDialect` | `PostgreSQLDialect` |
| 컨테이너 | `mariadb:11.2` | `postgres:16.2` |
| Flyway | 활성화 (mariadb 스크립트) | 활성화 (postgresql 스크립트) |

```bash
# 전체 테스트 실행
./gradlew test
```
