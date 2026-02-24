# Portfolio Backend — Production-Grade Spring Boot

> **GitHub**: https://github.com/kikjs75/oauth2_based_project

A production-ready backend portfolio demonstrating:
- OAuth2 JWT Assertion client (Google/Microsoft)
- FCM HTTP v1 push notifications
- JWT-based authentication with role escalation
- Multi-module Gradle architecture
- DB-agnostic persistence (MariaDB/PostgreSQL + Flyway)
- Observability: Micrometer, Prometheus, Grafana, Jaeger (OTLP)

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
# Edit .env with your values

# Start all services (MariaDB + Prometheus + Grafana + Jaeger + App)
docker-compose up -d
```

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
GOOGLE_SERVICE_ACCOUNT_KEY_PATH=/path/to/service-account.json
FCM_PROJECT_ID=my-firebase-project
```

## Security

- No real credentials are committed to the repository
- Service account JSON is loaded via file path only
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
