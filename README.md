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

| Module               | Description                                        |
| -------------------- | -------------------------------------------------- |
| `app`                | Main Spring Boot application                       |
| `oauth2-awt-core`    | OAuth2 JWT Bearer assertion library                |
| `oauth2-awt-starter` | Spring Boot auto-configuration for oauth2-awt-core |
| `fcm-client`         | FCM HTTP v1 API client                             |
| `frontend`           | React 18 + Vite SPA                                |

## Implementation

| 항목           | 내용                                                                      |
| -------------- | ------------------------------------------------------------------------- |
| **멀티모듈**   | `app`, `oauth2-awt-core`, `oauth2-awt-starter`, `fcm-client`              |
| **인증**       | ID/PW 회원가입·로그인 + Google OAuth2 Authorization Code, JWT(HS256) 발급 |
| **역할 관리**  | USER → WRITER 승격 (ADMIN only), 게시판 WRITER/ADMIN 전용                 |
| **게시판**     | 목록/상세/등록/수정/삭제, 본인 글 또는 ADMIN만 수정·삭제 가능             |
| **OAuth2 AWT** | RS256 JWT assertion, Caffeine 캐시, single-flight, retry                  |
| **FCM**        | HTTP v1 API, Micrometer Timer 메트릭                                      |
| **DB**         | MariaDB(기본)/PostgreSQL(옵션), Flyway `{vendor}` 자동 분기, DB-agnostic  |
| **모니터링**   | Prometheus + Grafana + Jaeger (docker-compose)                            |
| **프론트엔드** | React 18 + Vite, React Router v6, Axios, localStorage JWT                 |
| **보안**       | 실 credentials 미포함, 환경변수/파일 경로만 사용                          |
| **테스트**     | Testcontainers 통합테스트 (MariaDB/PostgreSQL), Mockito 단위테스트        |

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

### Docker Compose 전체 스택 실행 전 체크리스트

#### 1. `.env` — `OAUTH2_REDIRECT_URI` 변경 (필수)

`bootRun` 로컬 테스트용(`http://localhost:8080/api/posts`)과 Docker Compose용 값이 다릅니다.

| 실행 방식                 | `OAUTH2_REDIRECT_URI`             |
| ------------------------- | --------------------------------- |
| `bootRun` (Method A)      | `http://localhost:8080/api/posts` |
| Docker Compose (Method B) | `http://localhost:8081/callback`  |

`.env`를 다음과 같이 수정합니다.

```bash
OAUTH2_REDIRECT_URI=http://localhost:8081/callback
```

> `OAUTH2_REDIRECT_URI`는 **Spring이 JWT 발급 후 프론트엔드로 보내는 URL**입니다.
> Google → Spring으로 오는 redirect URI(`/login/oauth2/code/google`)와 다르며,
> GCP 콘솔에 등록할 필요가 없습니다.

#### 2. GCP 콘솔 — 추가 설정 불필요

GCP 콘솔에 등록된 `http://localhost:8080/login/oauth2/code/google`은 그대로 유지합니다.
`OAUTH2_REDIRECT_URI` 변경은 GCP 설정에 영향을 주지 않습니다.

#### 3. 포트 충돌 확인

Docker Compose 실행 전 아래 포트가 비어 있는지 확인합니다.

| 서비스     | 호스트 포트 | 확인 방법                           |
| ---------- | ----------- | ----------------------------------- |
| app        | 8080        | `bootRun` 실행 중이면 Ctrl+C로 종료 |
| frontend   | 8081        | npm dev 실행 중이면 종료            |
| mariadb    | 13306       | `docker compose down` 으로 정리     |
| grafana    | 3000        | —                                   |
| prometheus | 9090        | —                                   |
| jaeger     | 16686       | —                                   |

#### 4. 실행 순서

```bash
# (1) bootRun / npm dev 실행 중이면 종료 (Ctrl+C)

# (2) 기존 컨테이너 정리
docker compose down

# (3) OAUTH2_REDIRECT_URI가 올바르게 반영되는지 확인
docker compose config | grep OAUTH2_REDIRECT_URI
# 출력: OAUTH2_REDIRECT_URI=http://localhost:8081/callback

# (4) 전체 빌드 후 실행 (최초 또는 코드 변경 시)
docker compose up --build

# (5) 이미 빌드된 이미지 재사용 시
docker compose up
```

#### 5. 기동 확인

```bash
# 앱 헬스 체크
curl http://localhost:8080/actuator/health
# 정상: {"status":"UP"}
```

브라우저에서 프론트엔드 접속: `http://localhost:8081`

#### 6. Google OAuth2 테스트 (전체 스택)

브라우저에서 `http://localhost:8081/login` 접속 후 **Google로 로그인** 버튼 클릭.

Google 로그인 완료 후 흐름:

```
Google → Spring (/login/oauth2/code/google)
       → JWT 발급
       → http://localhost:8081/callback?token=eyJhbGci...
       → CallbackPage가 토큰을 localStorage에 저장
       → /posts 로 이동 (게시판 목록)
```

| 확인 항목                            | 성공 기준                                                 |
| ------------------------------------ | --------------------------------------------------------- |
| `/posts` 페이지로 이동               | OAuth2 로그인 + JWT 저장 성공                             |
| 게시글 목록 표시 (빈 목록 `[]` 포함) | API 호출 성공                                             |
| `redirect_uri_mismatch` 오류         | GCP 등록 URI 확인 (`/login/oauth2/code/google`)           |
| 로그인 후 `/login`으로 돌아옴        | `OAUTH2_REDIRECT_URI` 또는 `GOOGLE_CLIENT_ID/SECRET` 확인 |

#### 7. 정상 기동 로그 확인 포인트

`docker compose up --build` 성공 시 아래 로그가 순서대로 출력됩니다.

```
✔ Container portfolio-mariadb  Healthy          ← MariaDB 헬스체크 통과
✔ Container portfolio-app      Recreated        ← 앱 컨테이너 시작
✔ Container portfolio-frontend Recreated        ← 프론트엔드 컨테이너 시작

portfolio-app | HikariPool-1 - Start completed.                     ← DB 연결 성공
portfolio-app | Successfully validated 2 migrations                  ← Flyway V1, V2 정상
portfolio-app | Schema `portfolio` is up to date.                    ← DB 스키마 최신
portfolio-app | Started PortfolioApplication in 2.xxx seconds        ← 앱 기동 완료
portfolio-app | Tomcat started on port 8080                          ← API 서버 준비
portfolio-frontend | Configuration complete; ready for start up      ← nginx 준비 완료
```

이후 로그가 멈추고 아래 프롬프트가 표시되면 전체 스택 준비 완료입니다.

```
v View in Docker Desktop   o View Config   w Enable Watch   d Detach
```

#### 8. 브라우저 접속 및 기능 테스트

| 서비스              | URL                                   |
| ------------------- | ------------------------------------- |
| 프론트엔드 (게시판) | http://localhost:8081                 |
| 백엔드 헬스 체크    | http://localhost:8080/actuator/health |
| Grafana             | http://localhost:3000 (admin / admin) |
| Jaeger              | http://localhost:16686                |
| Prometheus          | http://localhost:9090                 |

**테스트 순서**:

1. `http://localhost:8081` 접속 → 회원가입 (`/signup`)
2. 로그인 (`/login`) — ID/PW 또는 **Google로 로그인** 버튼
3. 게시글 목록 확인 (`/posts`)
4. WRITER 역할 부여 후 게시글 작성 테스트

#### WRITER 역할 부여 방법

이 앱에는 두 가지 로그인 방식이 있습니다. `username`/`password`는 Google 계정이 아니라 **`/signup`에서 직접 가입한 ID/PW**입니다.

| 방식                 | 가입 방법               | username / password  |
| -------------------- | ----------------------- | -------------------- |
| ID/PW 로그인         | `/signup`에서 직접 가입 | 본인이 입력한 값     |
| Google OAuth2 로그인 | Google 버튼 클릭        | 없음 (Google이 처리) |

WRITER 역할 부여는 **ADMIN 권한을 가진 계정의 JWT**가 필요합니다. 앱에 ADMIN 생성 UI가 없으므로 DB에서 직접 역할을 변경합니다.

**Step 1 — ADMIN 계정 만들기 (DB 직접 수정)**

```bash
# MariaDB 접속
docker exec -it portfolio-mariadb mariadb -u portfolio -pportfolio portfolio

# 가입한 유저 확인
SELECT id, username, provider FROM users;
SELECT * FROM user_roles;

# 특정 유저를 ADMIN으로 변경 (id는 위 SELECT 결과 참고)
UPDATE user_roles SET role = 'ROLE_ADMIN' WHERE user_id = 1;
```

> Google OAuth2로만 가입한 경우 password가 없으므로 `/signup`에서 별도 ID/PW 계정을 하나 더 만든 후 해당 계정을 ADMIN으로 변경합니다.
> example : admin / Djemals1!(어드민1!)

**Step 2 — ADMIN 계정으로 로그인해서 JWT 받기**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"signup에서_가입한_ID","password":"가입할때_쓴_PW"}' | jq .
# 응답의 token 값을 복사
```

**Step 3 — WRITER 부여**

```bash
# userId는 Step 1의 SELECT 결과에서 확인 (숫자 ID)
curl -X POST http://localhost:8080/api/admin/users/1/grant-writer \
  -H "Authorization: Bearer eyJhbGci..."
```

> **자주 하는 실수**
>
> | 잘못된 예 | 올바른 예 |
> |---|---|
> | `/users/admin/grant-writer` (username) | `/users/1/grant-writer` (숫자 userId) |
> | `Bearer {eyJhbGci...}` (중괄호 포함) | `Bearer eyJhbGci...` (중괄호 없이) |
>
> 빈 응답(아무것도 출력 안 됨) = **성공** (HTTP 204 No Content)
> `INTERNAL_ERROR` 응답 = URL에 username이 들어간 경우

로그인과 WRITER 부여를 한 번에 처리하는 방법:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Djemals1!"}' | jq -r '.accessToken')

curl -X POST http://localhost:8080/api/admin/users/1/grant-writer \
  -H "Authorization: Bearer $TOKEN"

jinsu.kim@jinsukimui-MacBookPro 1-oauth2-based-project % curl -X POST http://localhost:8080/api/admin/users/1/grant-writer \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZXhwIjoxNzcyMDk2MzkwLCJpYXQiOjE3NzIwOTI3OTAsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwidXNlcm5hbWUiOiJhZG1pbiJ9.yu6T2qvqH2Gxz4FVpLcZSRRG8jEeakL_9NyrAu_S4Q0"
jinsu.kim@jinsukimui-MacBookPro 1-oauth2-based-project %  
```

WRITER 부여 후 게시글 작성(`/posts/new`)이 활성화됩니다.

### Run locally (development)

```bash
# 1. 의존 서비스 기동 (MariaDB)
docker compose up mariadb -d

# 2. 환경변수 로드
set -a && source .env && set +a

# 3. 백엔드 실행 (http://localhost:8080)
./gradlew :app:bootRun

# 4. 프론트엔드 실행 (별도 터미널, http://localhost:8081)
cd frontend
npm install
npm run dev
```

> 프론트엔드 dev 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

### 로컬 실행 시 참고사항

**`bootRun` 이 94%에서 멈춘 것처럼 보이는 경우**

정상입니다. `bootRun` 은 앱이 실행 중인 동안 계속 94%에 머뭅니다. 100%가 되는 건 앱이 종료될 때입니다.

**`HikariPool - Thread starvation or clock leap detected` 경고**

Mac이 잠자기(sleep) 상태였다가 깨어날 때 JVM이 시간 차이를 감지하는 경고입니다. 기능상 문제 없습니다.

**VS Code DevContainer 사용 시 포트 충돌**

VS Code가 MariaDB 포트를 자동으로 포워딩하여 Spring이 연결하지 못하는 경우가 있습니다.
VS Code Settings (`Cmd+,`) 에서 `remote.autoForwardPorts` 를 비활성화하면 해결됩니다.

### 앱 기동 확인

새 터미널에서 확인합니다.

```bash
curl http://localhost:8080/actuator/health
# 정상: {"status":"UP"}
```

### Google OAuth2 테스트

브라우저에서 접속합니다.

```
http://localhost:8080/oauth2/authorization/google
```

Google 로그인 완료 후 아래와 같이 리다이렉트되면 성공입니다.

```
http://localhost:8080/api/posts?token=eyJhbGci...
```

| 확인 항목                    | 성공 기준                    |
| ---------------------------- | ---------------------------- |
| URL에 `?token=eyJ...` 포함   | JWT 발급 성공                |
| 응답 본문 `[]`               | 게시글 없음 (정상)           |
| `redirect_uri_mismatch` 오류 | GCP 등록 URI 확인 필요       |
| `invalid_client` 오류        | Client ID / Secret 확인 필요 |

### JWT 내용 확인 (jwt.io)

URL에서 `token=` 뒤의 값을 복사한 후 [https://jwt.io](https://jwt.io) 에 붙여넣으면 JWT 내용을 확인할 수 있습니다.

```
# URL 예시
http://localhost:8080/api/posts?token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...
                                       ↑ 이 부분 전체를 복사
```

jwt.io Debugger 의 **Encoded** 칸에 붙여넣으면 **Decoded** 에서 아래 내용이 보입니다.

```json
// Header
{
  "alg": "HS256"
}

// Payload
{
  "sub": "1",                ← 우리 DB의 userId
  "roles": ["ROLE_USER"],   ← 부여된 역할
  "iat": 1772088570,        ← 발급 시각 (Unix timestamp)
  "exp": 1772092170         ← 만료 시각 (발급 후 1시간)
}
```

## API Endpoints

### 인증

| Method | Path                         | Role   | Description                                    |
| ------ | ---------------------------- | ------ | ---------------------------------------------- |
| POST   | /api/auth/signup             | Public | ID/PW 회원가입                                 |
| POST   | /api/auth/login              | Public | ID/PW 로그인 → JWT 반환                        |
| GET    | /oauth2/authorization/google | Public | Google OAuth2 로그인 시작                      |
| GET    | /login/oauth2/code/google    | Public | Google OAuth2 콜백 (Spring Security 자동 처리) |

### 관리자

| Method | Path                               | Role  | Description            |
| ------ | ---------------------------------- | ----- | ---------------------- |
| POST   | /api/admin/users/{id}/grant-writer | ADMIN | 사용자를 WRITER로 승격 |

### 게시판

| Method | Path            | Role                | Description      |
| ------ | --------------- | ------------------- | ---------------- |
| GET    | /api/posts      | Authenticated       | 게시글 목록 조회 |
| GET    | /api/posts/{id} | Authenticated       | 게시글 상세 조회 |
| POST   | /api/posts      | WRITER, ADMIN       | 게시글 등록      |
| PUT    | /api/posts/{id} | WRITER(본인), ADMIN | 게시글 수정      |
| DELETE | /api/posts/{id} | WRITER(본인), ADMIN | 게시글 삭제      |

### 푸시

| Method | Path           | Role          | Description          |
| ------ | -------------- | ------------- | -------------------- |
| POST   | /api/push/test | WRITER, ADMIN | FCM 테스트 푸시 발송 |

## Frontend

### 화면 구성

| 경로              | 화면                         | 접근                |
| ----------------- | ---------------------------- | ------------------- |
| `/signup`         | 회원가입                     | 비로그인            |
| `/login`          | 로그인 (ID/PW + Google 버튼) | 비로그인            |
| `/callback`       | OAuth2 토큰 처리 (자동 이동) | —                   |
| `/posts`          | 게시글 목록                  | 로그인              |
| `/posts/:id`      | 게시글 상세 + 수정/삭제 버튼 | 로그인              |
| `/posts/new`      | 게시글 작성                  | WRITER, ADMIN       |
| `/posts/:id/edit` | 게시글 수정                  | WRITER(본인), ADMIN |

### 권한별 UI 동작

- 글쓰기 버튼: WRITER 또는 ADMIN 역할일 때만 목록 화면에 노출
- 수정/삭제 버튼: 본인이 작성한 글이거나 ADMIN일 때만 상세 화면에 노출
- Google OAuth2 로그인 성공 시 `/callback?token=<JWT>` 로 리다이렉트되어 토큰을 localStorage에 저장 후 게시판으로 이동

### 기술 스택

| 항목            | 내용                               |
| --------------- | ---------------------------------- |
| 빌드 도구       | Vite 5                             |
| 프레임워크      | React 18                           |
| 라우팅          | React Router v6                    |
| HTTP 클라이언트 | Axios (JWT Bearer 인터셉터)        |
| 상태 관리       | localStorage (JWT 저장)            |
| 스타일          | 순수 CSS (외부 UI 라이브러리 없음) |

## 포트 정리

| 서비스     | URL                    | 비고                             |
| ---------- | ---------------------- | -------------------------------- |
| 프론트엔드 | http://localhost:8081  | React (Docker: nginx, Dev: Vite) |
| 백엔드 API | http://localhost:8080  | Spring Boot                      |
| Prometheus | http://localhost:9090  | 메트릭 수집                      |
| Grafana    | http://localhost:3000  | admin / admin                    |
| Jaeger     | http://localhost:16686 | 분산 트레이싱                    |

## Monitoring

Prometheus, Grafana, Jaeger는 docker-compose에 포함되어 있습니다.
포트는 [포트 정리](#포트-정리) 섹션을 참고하세요.

- **Prometheus**: `/actuator/prometheus` 엔드포인트 스크레이핑
- **Grafana**: Prometheus 데이터소스 자동 프로비저닝 (`monitoring/grafana/provisioning/`)
- **Jaeger**: OTLP(gRPC/HTTP) 수신, 분산 트레이스 시각화

## Configuration

### 환경변수 출처

| 환경변수                          | 출처                                 | 설명                                             |
| --------------------------------- | ------------------------------------ | ------------------------------------------------ |
| `GOOGLE_CLIENT_ID`                | GCP 콘솔 → OAuth2 클라이언트         | OAuth2 앱 식별자                                 |
| `GOOGLE_CLIENT_SECRET`            | GCP 콘솔 → OAuth2 클라이언트         | OAuth2 앱 시크릿                                 |
| `JWT_SECRET`                      | 직접 생성                            | 우리 앱이 JWT 서명할 때 사용하는 키 (GCP와 무관) |
| `GOOGLE_SERVICE_ACCOUNT_KEY_PATH` | GCP 콘솔 → 서비스 계정 JSON 다운로드 | FCM/OAuth2 AWT용 서비스 계정                     |
| `FCM_PROJECT_ID`                  | Firebase 콘솔 → 프로젝트 설정        | FCM 프로젝트 ID                                  |

### JWT_SECRET 생성

GCP 등 외부 서비스에서 받는 값이 아니라 직접 생성합니다. 256비트(32바이트) 이상의 랜덤 문자열이면 됩니다.

```bash
openssl rand -base64 32
# 출력 예: K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

### GCP OAuth2 설정 (Google Cloud Console)

| 항목                   | 값                                               |
| ---------------------- | ------------------------------------------------ |
| 승인된 JavaScript 원본 | (불필요 — 백엔드가 Google과 직접 통신)           |
| 승인된 리다이렉션 URI  | `http://localhost:8080/login/oauth2/code/google` |

> **리다이렉트 2단계 구분**
>
> - 1단계 (GCP 등록 URI): Google → Spring으로 인증 code 전달 (`/login/oauth2/code/google`)
> - 2단계 (`OAUTH2_REDIRECT_URI`): Spring이 JWT 발급 후 → 프론트엔드로 전달 (`/callback?token=JWT`)
>
> Google은 2단계를 모릅니다. 1단계 URI만 GCP 콘솔에 등록하면 됩니다.

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

| 항목                                        | 로컬 개발 (`bootRun`)                                        | Docker Compose                                                   |
| ------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------- |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | `.env`에 설정 (미설정 시 OAuth2 로그인 불가, 앱 기동 O)      | 동일                                                             |
| `OAUTH2_REDIRECT_URI`                       | 프론트엔드 콜백 URL (기본: `http://localhost:8081/callback`) | 동일                                                             |
| `GOOGLE_SERVICE_ACCOUNT_KEY_PATH`           | 호스트 경로 직접 참조                                        | 호스트 경로 → `/secrets/google-service-account.json` 자동 마운트 |
| FCM 미설정 시                               | 앱 기동 O, push 엔드포인트만 비활성                          | 동일                                                             |
| Microsoft                                   | 선택 사항, 미설정 시 빈 비활성                               | 동일                                                             |

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

### registrationId

`/oauth2/authorization/google` 의 `google` 부분은 `application.yml` 에서 개발자가 붙인 이름(**registrationId**)입니다.
프로젝트 코드 어디에도 이 경로를 직접 정의하지 않으며, Spring Security 내부에서 자동 생성합니다.

```yaml
# app/src/main/resources/application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:          ← registrationId = "google"
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
```

```
/oauth2/authorization/{registrationId}  →  /oauth2/authorization/google   (로그인 시작)
/login/oauth2/code/{registrationId}     →  /login/oauth2/code/google      (Google 콜백)
```

### GCP 콘솔 설정

| 항목                      | 값                                               |
| ------------------------- | ------------------------------------------------ |
| 승인된 JavaScript 원본    | (불필요 — 백엔드가 Google과 직접 통신)           |
| **승인된 리다이렉션 URI** | `http://localhost:8080/login/oauth2/code/google` |

> **리다이렉트 2단계 구분**
>
> - 1단계: Google → Spring (`/login/oauth2/code/google`) — AUTH_CODE 전달, GCP 콘솔에 등록
> - 2단계: Spring → 프론트엔드 (`OAUTH2_REDIRECT_URI`) — JWT 전달, Google은 알지 못함

### OAuth2 vs OIDC — scope: openid 의 의미

`application.yml`에 `scope: openid`가 포함되면 Google은 일반 OAuth2가 아닌 **OIDC(OpenID Connect)** 플로우로 동작합니다.

**OAuth2만 사용할 때 (`scope: email, profile`)**

```
구글 → access_token 발급
내 앱 → access_token으로 /userinfo 엔드포인트 직접 호출 → 이메일, 이름 수동으로 조회
```

**OIDC 사용 시 (`scope: openid, email, profile`)**

```
구글 → access_token + id_token 함께 발급
        └─ id_token: 구글이 서명한 JWT (sub, email, name 이미 포함)
내 앱 → id_token만 검증하면 됨 (별도 /userinfo 호출 불필요)
```

**Spring Security 내부 분기**

| | `userService` | `oidcUserService` |
|---|---|---|
| 호출 조건 | `openid` 스코프 **없을 때** | `openid` 스코프 **있을 때** |
| 입력 타입 | `OAuth2UserRequest` | `OidcUserRequest` (`id_token` 포함) |

`scope: openid`가 있으면 Spring Security는 `oidcUserService`를 호출합니다.
`userService`만 등록하고 `oidcUserService`를 등록하지 않으면 커스텀 로직이 무시되고
기본 `DefaultOidcUserService`가 동작합니다 — DB 저장 안 됨, `userId = null` JWT 발급.

**이 프로젝트의 해결 방법** (`SecurityConfig.java`)

```java
OidcUserService oidcUserService = new OidcUserService();
oidcUserService.setOauth2UserService(googleOAuth2UserService); // DB 저장 로직 위임

.userInfoEndpoint(ui -> {
    ui.userService(googleOAuth2UserService);   // openid 없을 때
    ui.oidcUserService(oidcUserService);       // openid 있을 때 → googleOAuth2UserService 에 위임
})
```

### 현재 프로젝트는 OIDC 플로우로 동작한다

`application.yml`에 `scope: openid`가 포함되어 있으므로 현재 프로젝트는 **OIDC 플로우**입니다.

```
scope: openid, email, profile
  → Spring Security: OIDC 플로우 선택
  → OidcUserService.loadUser() 호출
  → 내부적으로 googleOAuth2UserService 에 위임
  → GoogleOAuth2UserService.fetchFromGoogle() = super.loadUser()
      → DefaultOAuth2UserService 가 Google /userinfo 엔드포인트 자동 호출
      → { sub, email, name, ... } 응답
  → DB find-or-create → JWT 발급
```

**userinfo 호출은 수동이 아니라 자동**입니다. `super.loadUser()`(`DefaultOAuth2UserService`) 가 내부적으로 Google `/userinfo` 엔드포인트에 HTTP 요청을 보내고 응답을 파싱합니다. 개발자가 직접 HTTP 요청을 작성할 필요가 없습니다.

`GoogleOAuth2UserService`에서 `fetchFromGoogle()`을 별도 메서드로 분리한 이유도 여기에 있습니다 — 테스트에서 이 HTTP 호출만 Mock으로 대체할 수 있도록 하기 위해서입니다.

| | 누가 처리 | userinfo 호출 |
|---|---|---|
| OIDC (현재) | `OidcUserService` → 위임 → `GoogleOAuth2UserService` | 자동 (`super.loadUser()`) |
| 일반 OAuth2 | `GoogleOAuth2UserService` 직접 | 자동 (`super.loadUser()`) |

구글이 OIDC 응답으로 `id_token`도 함께 보내는지 여부로 플로우를 구분할 수 있습니다:

```
OIDC 응답:   { access_token: "...", id_token: "eyJ...", token_type: "Bearer" }
OAuth2 응답: { access_token: "...",                     token_type: "Bearer" }
```

## Database Support

Default: **MariaDB**

Flyway 마이그레이션은 `{vendor}` 플레이스홀더로 DB별 스크립트를 자동 선택합니다:

| 버전 | 내용                                                                                   |
| ---- | -------------------------------------------------------------------------------------- |
| V1   | 초기 스키마 (users, user_roles, posts)                                                 |
| V2   | username 255자 확장, password nullable, provider / provider_id 컬럼 추가 (OAuth2 지원) |

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

| 항목           | MariaDB 테스트                                 | PostgreSQL 테스트            |
| -------------- | ---------------------------------------------- | ---------------------------- |
| 베이스 클래스  | `MariaDbContainerSupport`                      | `PostgresContainerSupport`   |
| 프로파일       | `test`                                         | `test-pg`                    |
| yml            | `application-test.yml`                         | `application-test-pg.yml`    |
| dialect        | `MariaDBDialect`                               | `PostgreSQLDialect`          |
| 컨테이너       | `mariadb:11.2`                                 | `postgres:16.2`              |
| Flyway         | 활성화 (mariadb 스크립트)                      | 활성화 (postgresql 스크립트) |
| Testcontainers | BOM 1.21.4 (docker-java 3.4.1, API v1.44 지원) | 동일                         |

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

### Gradle 명령어 레퍼런스

**전체 / 모듈별 실행**

```bash
# 전체 (모든 모듈)
./gradlew test

# 모듈별 개별 실행
./gradlew :app:test
./gradlew :fcm-client:test
./gradlew :oauth2-awt-core:test
```

**주요 옵션**

| 옵션                     | 설명                                                                |
| ------------------------ | ------------------------------------------------------------------- |
| `--no-daemon`            | 백그라운드 데몬 없이 새 JVM으로 실행. 데몬 캐시 영향 배제할 때 사용 |
| `--refresh-dependencies` | `~/.gradle/caches` 무시하고 원격 저장소에서 의존성 재다운로드       |
| `-i`                     | INFO 레벨 로그 출력 (Docker 연결 흐름, 테스트 상세 등 확인 시)      |

```bash
# 데몬 제외, 캐시 무시, 상세 로그
./gradlew :app:test --no-daemon --refresh-dependencies -i
```

**의존성 분석**

```bash
# 특정 configuration 전체 의존성 트리
./gradlew :app:dependencies --configuration testRuntimeClasspath

# 특정 라이브러리 유입 경로 추적 (버전 충돌 분석)
./gradlew :app:dependencyInsight --dependency testcontainers --configuration testRuntimeClasspath

# 의존성 강제 재다운로드
./gradlew :app:dependencies --configuration testRuntimeClasspath --refresh-dependencies
```

**정리**

```bash
# 빌드 산출물 삭제
./gradlew clean

# 실행 중인 Gradle 데몬 모두 종료
./gradlew --stop
```
