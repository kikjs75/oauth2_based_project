# Portfolio Backend — Production-Grade Spring Boot

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
