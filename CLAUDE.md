# CLAUDE.md — Production-Grade Portfolio Build Instructions

## 0) Goal (What to build)

This repository is a production-grade backend portfolio project.

It must demonstrate:

- Secure OAuth2 assertion-based token client
- FCM HTTP v1 integration
- Multi-module architecture
- DB-agnostic persistence
- JWT-based authentication
- Observability (metrics + tracing)
- Production-ready monitoring stack
- Clean Git history with structured commits

---

# 1) Main Demo Application

Spring Boot multi-module project.

### Features

- User signup/login (JWT-based authentication)
- Role escalation (USER → WRITER)
- Board posting (WRITER only)
- Admin endpoint to grant WRITER role (mandatory)
- Push notification demo endpoint
- Uses OAuth2 AWT module to obtain Google access token
- Sends push using FCM HTTP v1 API

---

# 2) OAuth2 AWT Module

Separate reusable library module implementing OAuth2 JWT Assertion flow.

### Responsibilities

- Build JWT using Nimbus JOSE + JWT
- RS256 signing
- POST assertion to Google and Microsoft token endpoints
- Access token caching (Caffeine)
- Refresh control with skew
- Retry + timeout
- Clock skew tolerance (±60s)
- Concurrency protection (single-flight refresh)

---

# 3) Non-Goals

- No full Authorization Code UI
- No frontend SPA
- No real credentials in repository

---

# 4) Tech Stack (Mandatory)

- Java 17+
- Spring Boot 3.x
- Gradle multi-module
- Spring Security (JWT authentication)
- Nimbus JOSE + JWT
- Spring RestClient
- Flyway
- Caffeine
- Micrometer
- OpenTelemetry bridge
- Prometheus
- Grafana
- Jaeger

---

# 5) Persistence

## Default DB
- MariaDB (docker-compose)

## Alternative DB
- PostgreSQL (fully supported)

Requirements:
- DB-agnostic design
- Config-only DB switching
- Flyway support for both
- Testcontainers support for both

---

# 6) Observability (MANDATORY)

## Metrics
- Micrometer enabled
- Prometheus endpoint exposed
- Metrics include OAuth, cache, FCM, HTTP latency

## Monitoring Stack
docker-compose MUST include:
- MariaDB
- Optional PostgreSQL
- Prometheus
- Grafana
- Jaeger

## Tracing
- Trace ID required
- OTLP export to Jaeger
- Logs must include traceId and spanId

---

# 7) Security Requirements

- NEVER commit real credentials
- Use environment variables
- application.yml must contain placeholders only

## Key Policy

Google:
- Service account JSON via file path only

Microsoft:
- Client certificate/private key via PEM file only

---

# 8) Functional Requirements

Auth (JWT-based):
- POST /api/auth/signup
- POST /api/auth/login
- POST /api/admin/users/{id}/grant-writer
- POST /api/posts
- GET /api/posts
- GET /api/posts/{id}

Roles:
- USER
- WRITER
- ADMIN

Push:
- POST /api/push/test
- Allowed for WRITER or ADMIN

---

# 9) Testing Strategy

Unit:
- JWT signing
- Cache logic
- Concurrency guard
- Retry logic

Integration:
- WireMock (token endpoints)
- Testcontainers (MariaDB default, PostgreSQL optional)

CI must not require secrets.

---

# 10) Build & Version Control Policy (MANDATORY)

## Step 0: Initialize Git

1. Create repository
2. Add .gitignore
3. Initial commit:
   chore: initialize repository structure
4. Push to remote

---

## Development Rules

- Small commits only
- One logical change per commit
- Push after each milestone
- Use clear commit messages

---

## Step-by-step Build Plan

1. Create a Git repository and perform step-by-step commits and pushes.

2. Bootstrap multi-module structure  
   → feat: bootstrap multi-module structure

3. Implement JWT auth + DB + Flyway
   → feat: implement JWT auth and schema

4. Implement board + role logic
   → feat: add role-based board

5. Implement oauth2-awt-core
   → feat: implement OAuth2 assertion core

6. Implement starter
   → feat: add Spring Boot starter

7. Implement fcm-client
   → feat: add FCM client module

8. Integrate push endpoint
   → feat: integrate push endpoint

9. Configure monitoring stack
   → feat: add monitoring stack

10. Complete documentation
   → docs: finalize documentation

---

# 11) AI Usage & Commit Policy (MANDATORY)

AI tools (including Claude) may assist with:

- Code generation
- Refactoring suggestions
- Documentation drafting

However:

- The developer remains the sole author of the repository.
- The repository must not include:
  Co-authored-by: Claude <noreply@anthropic.com>
- No automatic AI co-author trailer is allowed in commit messages.

If such trailer is automatically added, it must be removed before committing.

Recommended: configure a Git commit-msg hook to automatically remove:

  Co-authored-by: Claude <noreply@anthropic.com>

The commit history must reflect clean, professional authorship.

---

# 12) Coding Conventions

- Package-by-feature
- Constructor injection
- Use records for DTOs
- Jakarta validation
- Error response:
  - error_code
  - message
  - trace_id

---

# 13) What Claude May Ask

Claude may ask only:
- Google scopes
- Minimal schema decisions

Claude must never ask for real secrets.
