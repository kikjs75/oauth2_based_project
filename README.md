# Portfolio — Production-Grade Spring Boot + React

> **GitHub**: https://github.com/kikjs75/oauth2_based_project

A full-stack portfolio project demonstrating production-ready practices.

**Backend** (Spring Boot multi-module):
- OAuth2 JWT Assertion client (Google/Microsoft)
- Google OAuth2 Authorization Code login
- FCM HTTP v1 push notifications
- JWT-based authentication with role escalation
- Multi-module Gradle architecture
- DB-agnostic persistence (MariaDB/PostgreSQL + Flyway)
- Observability: Micrometer, Prometheus, Grafana, Jaeger (OTLP)

**Frontend** (React + Vite):
- 회원가입 / 로그인 (ID/PW + Google OAuth2)
- 게시판 목록 / 상세 / 등록 / 수정 / 삭제

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
│       │   │   ├── auth/                          ← 회원가입 / 로그인 / Google OAuth2 / JWT 발급
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── GoogleOAuth2UserService.java          ← Google 사용자 find-or-create
│       │   │   │   ├── OAuth2AuthenticationSuccessHandler.java ← JWT 발급 후 프론트엔드로 리다이렉트
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
│       │   │   │       ├── UpdatePostRequest.java
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
│       │           ├── mariadb/
│       │           │   ├── V1__init_schema.sql
│       │           │   └── V2__add_oauth2_provider.sql
│       │           └── postgresql/
│       │               ├── V1__init_schema.sql
│       │               └── V2__add_oauth2_provider.sql
│       └── test/
│           ├── java/com/portfolio/app/
│           │   ├── admin/
│           │   │   └── AdminServiceTest.java                ← Mockito: WRITER 역할 부여 단위테스트
│           │   ├── auth/
│           │   │   ├── AuthIntegrationTest.java             ← MariaDB 통합 테스트 (signup/login)
│           │   │   ├── AuthPostgresIntegrationTest.java     ← PostgreSQL 통합 테스트 (signup/login)
│           │   │   ├── GoogleOAuth2UserServiceTest.java     ← Mockito: find-or-create 3개 케이스
│           │   │   └── OAuth2AuthenticationSuccessHandlerTest.java ← JWT 발급 및 리다이렉트 검증
│           │   ├── post/
│           │   │   └── PostServiceTest.java                 ← Mockito: 게시글 CRUD 권한 단위테스트
│           │   ├── push/
│           │   │   └── PushServiceTest.java                 ← Mockito: FCM 호출 단위테스트
│           │   ├── security/
│           │   │   ├── JwtAuthenticationFilterTest.java     ← JWT 필터 단위테스트
│           │   │   └── JwtTokenProviderTest.java            ← JWT 발급/검증 단위테스트
│           │   └── support/
│           │       ├── MariaDbContainerSupport.java         ← GenericContainer + WaitAllStrategy + HostOS getMappedPort
│           │       └── PostgresContainerSupport.java        ← GenericContainer + log wait + HostOS getMappedPort
│           └── resources/
│               ├── application-test.yml                     ← MariaDB 테스트 프로파일
│               ├── application-test-pg.yml                  ← PostgreSQL 테스트 프로파일
│               └── testcontainers.properties                ← Docker 소켓 / 클라이언트 전략 고정
│
├── oauth2-awt-core/                       ← OAuth2 JWT Assertion 라이브러리 (재사용 가능)
│   ├── build.gradle
│   ├── src/main/java/com/portfolio/oauth2/awt/core/
│   │   ├── AssertionConfig.java           ← Google/Microsoft 설정 (Builder 패턴)
│   │   ├── AssertionTokenClient.java      ← 공개 API 진입점
│   │   ├── CachedTokenProvider.java       ← Caffeine 캐시 + ReentrantLock single-flight
│   │   ├── JwtAssertionBuilder.java       ← RS256 JWT 서명 (Nimbus JOSE)
│   │   ├── TokenEndpointClient.java       ← 토큰 엔드포인트 HTTP 호출 (RestClient)
│   │   └── TokenResponse.java             ← 액세스 토큰 + 만료시각, isExpiredWithSkew() 헬퍼
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
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── fcm-client/                            ← FCM HTTP v1 클라이언트
│   ├── build.gradle
│   ├── src/main/java/com/portfolio/fcm/
│   │   ├── FcmClient.java                 ← Bearer 토큰 획득 후 FCM API 호출, Micrometer Timer
│   │   ├── FcmConfig.java                 ← fcm.* 설정 바인딩
│   │   └── FcmMessage.java                ← FCM HTTP v1 메시지 페이로드
│   └── src/test/java/com/portfolio/fcm/
│       └── FcmClientTest.java             ← WireMock: FCM API 호출 단위테스트
│
├── monitoring/                            ← 모니터링 스택 설정
│   ├── prometheus/prometheus.yml          ← /actuator/prometheus 스크레이핑
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml     ← Prometheus 데이터소스 자동 프로비저닝
│       └── dashboards/dashboard.yml       ← 대시보드 프로바이더 설정
│
├── frontend/                              ← React 18 + Vite 프론트엔드
│   ├── Dockerfile                         ← 멀티스테이지 빌드 (node:18 → nginx:alpine)
│   ├── nginx.conf                         ← SPA 폴백, /api 프록시 → app:8080
│   ├── package.json
│   ├── vite.config.js                     ← dev 포트 8081, /api 프록시 → localhost:8080
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx                        ← React Router 라우팅 정의
│       ├── index.css
│       ├── api/
│       │   └── client.js                  ← Axios 인스턴스 + JWT 인터셉터 + 파싱 헬퍼
│       ├── components/
│       │   └── Navbar.jsx
│       └── pages/
│           ├── SignupPage.jsx             ← ID/PW 회원가입
│           ├── LoginPage.jsx             ← ID/PW 로그인 + Google 로그인 버튼
│           ├── CallbackPage.jsx          ← OAuth2 리다이렉트 토큰 수신 후 저장
│           ├── PostListPage.jsx          ← 게시글 목록
│           ├── PostDetailPage.jsx        ← 게시글 상세 + 수정/삭제 버튼
│           └── PostFormPage.jsx          ← 게시글 작성/수정 폼
│
├── build.gradle          ← 루트 공통 의존성 / BOM 관리
├── settings.gradle       ← 멀티모듈 서브프로젝트 선언
├── docker-compose.yml    ← 전체 스택 (MariaDB, Prometheus, Grafana, Jaeger, App, Frontend)
├── Dockerfile            ← 백엔드 멀티스테이지 빌드 (Gradle → JRE 17 최소 이미지)
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
| `frontend` | React 18 + Vite SPA |

## Implementation

| 항목 | 내용 |
|---|---|
| **멀티모듈** | `app`, `oauth2-awt-core`, `oauth2-awt-starter`, `fcm-client` |
| **인증** | ID/PW 회원가입·로그인 + Google OAuth2 Authorization Code, JWT(HS256) 발급 |
| **역할 관리** | USER → WRITER 승격 (ADMIN only), 게시판 WRITER/ADMIN 전용 |
| **게시판** | 목록/상세/등록/수정/삭제, 본인 글 또는 ADMIN만 수정·삭제 가능 |
| **OAuth2 AWT** | RS256 JWT assertion, Caffeine 캐시, single-flight, retry |
| **FCM** | HTTP v1 API, Micrometer Timer 메트릭 |
| **DB** | MariaDB(기본)/PostgreSQL(옵션), Flyway `{vendor}` 자동 분기, DB-agnostic |
| **모니터링** | Prometheus + Grafana + Jaeger (docker-compose) |
| **프론트엔드** | React 18 + Vite, React Router v6, Axios, localStorage JWT |
| **보안** | 실 credentials 미포함, 환경변수/파일 경로만 사용 |
| **테스트** | Testcontainers 통합테스트 (MariaDB/PostgreSQL), Mockito 단위테스트 |

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose

### Run with Docker Compose (전체 스택)
```bash
cp .env.example .env
# .env 필수 항목: JWT_SECRET, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
# 선택 항목: FCM_PROJECT_ID, GOOGLE_SERVICE_ACCOUNT_KEY_PATH

docker-compose up -d
```

> **FCM 설정 흐름**
> `.env`의 `GOOGLE_SERVICE_ACCOUNT_KEY_PATH`(호스트 경로)가 docker-compose volume으로
> 컨테이너 내부 `/secrets/google-service-account.json`에 자동 마운트됩니다.
> 미설정 시 FCM 빈이 비활성화되어 앱은 정상 기동되며, `/api/push/test`만 비활성 상태가 됩니다.

### Run locally (development)
```bash
# 1. 의존 서비스 기동 (MariaDB + Jaeger)
docker-compose up -d mariadb jaeger

# 2. 백엔드 실행 (http://localhost:8080)
./gradlew :app:bootRun

# 3. 프론트엔드 실행 (별도 터미널, http://localhost:8081)
cd frontend
npm install
npm run dev
```

> 프론트엔드 dev 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

## API Endpoints

### 인증
| Method | Path | Role | Description |
|---|---|---|---|
| POST | /api/auth/signup | Public | ID/PW 회원가입 |
| POST | /api/auth/login | Public | ID/PW 로그인 → JWT 반환 |
| GET | /oauth2/authorization/google | Public | Google OAuth2 로그인 시작 |
| GET | /login/oauth2/code/google | Public | Google OAuth2 콜백 (Spring Security 자동 처리) |

### 관리자
| Method | Path | Role | Description |
|---|---|---|---|
| POST | /api/admin/users/{id}/grant-writer | ADMIN | 사용자를 WRITER로 승격 |

### 게시판
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/posts | Authenticated | 게시글 목록 조회 |
| GET | /api/posts/{id} | Authenticated | 게시글 상세 조회 |
| POST | /api/posts | WRITER, ADMIN | 게시글 등록 |
| PUT | /api/posts/{id} | WRITER(본인), ADMIN | 게시글 수정 |
| DELETE | /api/posts/{id} | WRITER(본인), ADMIN | 게시글 삭제 |

### 푸시
| Method | Path | Role | Description |
|---|---|---|---|
| POST | /api/push/test | WRITER, ADMIN | FCM 테스트 푸시 발송 |

## Frontend

### 화면 구성

| 경로 | 화면 | 접근 |
|---|---|---|
| `/signup` | 회원가입 | 비로그인 |
| `/login` | 로그인 (ID/PW + Google 버튼) | 비로그인 |
| `/callback` | OAuth2 토큰 처리 (자동 이동) | — |
| `/posts` | 게시글 목록 | 로그인 |
| `/posts/:id` | 게시글 상세 + 수정/삭제 버튼 | 로그인 |
| `/posts/new` | 게시글 작성 | WRITER, ADMIN |
| `/posts/:id/edit` | 게시글 수정 | WRITER(본인), ADMIN |

### 권한별 UI 동작

- 글쓰기 버튼: WRITER 또는 ADMIN 역할일 때만 목록 화면에 노출
- 수정/삭제 버튼: 본인이 작성한 글이거나 ADMIN일 때만 상세 화면에 노출
- Google OAuth2 로그인 성공 시 `/callback?token=<JWT>` 로 리다이렉트되어 토큰을 localStorage에 저장 후 게시판으로 이동

### 기술 스택

| 항목 | 내용 |
|---|---|
| 빌드 도구 | Vite 5 |
| 프레임워크 | React 18 |
| 라우팅 | React Router v6 |
| HTTP 클라이언트 | Axios (JWT Bearer 인터셉터) |
| 상태 관리 | localStorage (JWT 저장) |
| 스타일 | 순수 CSS (외부 UI 라이브러리 없음) |

## 포트 정리

| 서비스 | URL | 비고 |
|---|---|---|
| 프론트엔드 | http://localhost:8081 | React (Docker: nginx, Dev: Vite) |
| 백엔드 API | http://localhost:8080 | Spring Boot |
| Prometheus | http://localhost:9090 | 메트릭 수집 |
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger | http://localhost:16686 | 분산 트레이싱 |

## Monitoring

Prometheus, Grafana, Jaeger는 docker-compose에 포함되어 있습니다.
포트는 [포트 정리](#포트-정리) 섹션을 참고하세요.

- **Prometheus**: `/actuator/prometheus` 엔드포인트 스크레이핑
- **Grafana**: Prometheus 데이터소스 자동 프로비저닝 (`monitoring/grafana/provisioning/`)
- **Jaeger**: OTLP(gRPC/HTTP) 수신, 분산 트레이스 시각화

## Configuration

All secrets are via environment variables (see `.env.example`):

```
JWT_SECRET=...
DB_URL=jdbc:mariadb://localhost:3306/portfolio
DB_USERNAME=portfolio
DB_PASSWORD=portfolio

# Google OAuth2 Login (Authorization Code flow)
GOOGLE_CLIENT_ID=your-google-oauth2-client-id
GOOGLE_CLIENT_SECRET=your-google-oauth2-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8081/callback

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
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | `.env`에 설정 (미설정 시 OAuth2 로그인 불가, 앱 기동 O) | 동일 |
| `OAUTH2_REDIRECT_URI` | 프론트엔드 콜백 URL (기본: `http://localhost:8081/callback`) | 동일 |
| `GOOGLE_SERVICE_ACCOUNT_KEY_PATH` | 호스트 경로 직접 참조 | 호스트 경로 → `/secrets/google-service-account.json` 자동 마운트 |
| FCM 미설정 시 | 앱 기동 O, push 엔드포인트만 비활성 | 동일 |
| Microsoft | 선택 사항, 미설정 시 빈 비활성 | 동일 |

## Security

- No real credentials are committed to the repository
- Google OAuth2 Login: client-id/secret via environment variables only
- Google service account JSON is loaded via file path only
- Microsoft private key is loaded via PEM file path only
- JWT secret via environment variable only

## Google OAuth2 Login Flow

```
Browser/Client
  → GET /oauth2/authorization/google          (Spring Security redirects to Google)
  → Google consent screen
  → GET /login/oauth2/code/google?code=...   (Spring Security handles callback)
  → GoogleOAuth2UserService: find-or-create user in DB
  → OAuth2AuthenticationSuccessHandler: issue JWT
  → redirect → {OAUTH2_REDIRECT_URI}?token={JWT}
  → Client uses token for subsequent API calls
```

Google Cloud Console에서 `http://localhost:8080/login/oauth2/code/google`을
Authorized redirect URI로 등록해야 합니다.

## Database Support

Default: **MariaDB**

Flyway 마이그레이션은 `{vendor}` 플레이스홀더로 DB별 스크립트를 자동 선택합니다:

| 버전 | 내용 |
|---|---|
| V1 | 초기 스키마 (users, user_roles, posts) |
| V2 | username 255자 확장, password nullable, provider / provider_id 컬럼 추가 (OAuth2 지원) |

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
| Testcontainers | BOM 1.21.4 (docker-java 3.4.1, API v1.44 지원) | 동일 |

### 실행 전제 조건

통합 테스트(`AuthIntegrationTest`, `AuthPostgresIntegrationTest`)는 **Docker Desktop이 실행 중**이어야 합니다.
단위 테스트는 Docker 없이 실행됩니다.

### Docker Desktop 29.x 호환성

Docker Desktop 29.x는 Docker API v1.44 이상을 요구합니다.
Testcontainers BOM 1.21.4를 사용하여 이 버전을 지원하며, `app/build.gradle`에서
`DOCKER_HOST`를 Gradle `environment` DSL로 명시합니다.
(shell 환경변수는 Gradle이 포크한 테스트 JVM에 자동 상속되지 않음)

`testcontainers.properties`에 Docker 소켓 경로와 클라이언트 전략이 고정되어 있어
별도 환경 설정 없이 Mac HostOS에서 바로 실행할 수 있습니다.

```bash
# 전체 테스트 (Docker Desktop 실행 필요)
./gradlew test

# 통합 테스트만
./gradlew :app:test --tests "com.portfolio.app.auth.AuthIntegrationTest"
./gradlew :app:test --tests "com.portfolio.app.auth.AuthPostgresIntegrationTest"

# 단위 테스트만 (Docker 불필요)
./gradlew :app:test --tests "com.portfolio.app.security.*" --tests "com.portfolio.app.admin.*" --tests "com.portfolio.app.post.*" --tests "com.portfolio.app.push.*" --tests "com.portfolio.app.auth.GoogleOAuth2UserServiceTest" --tests "com.portfolio.app.auth.OAuth2AuthenticationSuccessHandlerTest"
```
