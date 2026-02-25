# Debugging Log — 2026-02-25

## 전체 테스트 실행 결과 (2026-02-25) — HostOS 최종

### 실행 환경

- 환경: Mac HostOS (Apple Silicon, Docker Desktop 29.2.1)
- 명령: `./gradlew :app:test --no-daemon`
- 전체: **38개** / 통과: **38개** / 실패: **0개**

### 결과 요약

| 테스트 클래스 | 모듈 | DB | 결과 |
|---|---|---|---|
| `JwtTokenProviderTest` | app | — | PASS |
| `JwtAuthenticationFilterTest` | app | — | PASS |
| `OAuth2AuthenticationSuccessHandlerTest` | app | — | PASS |
| `GoogleOAuth2UserServiceTest` | app | — | PASS |
| `AdminServiceTest` | app | — | PASS |
| `PostServiceTest` | app | — | PASS |
| `PushServiceTest` | app | — | PASS |
| `FcmClientTest` | fcm-client | — | PASS |
| `JwtAssertionBuilderTest` | oauth2-awt-core | — | PASS |
| `CachedTokenProviderTest` | oauth2-awt-core | — | PASS |
| `AuthIntegrationTest` | app | MariaDB (Testcontainers) | PASS |
| `AuthPostgresIntegrationTest` | app | PostgreSQL (Testcontainers) | PASS |

### Sandbox 중간 결과 (Claude Code PinP 환경)

- 환경: Claude Code Sandbox (Docker 소켓 없음 — PinP 구조)
- 전체: **38개** / 통과: **34개** / 실패: **4개**
- 실패: `AuthIntegrationTest`, `AuthPostgresIntegrationTest` — Docker 소켓 접근 불가로 Testcontainers 기동 불가 (예상된 결과)

---

## 수정된 버그들 (2026-02-25)

### 8. Docker Desktop 29.x API 버전 불일치

**증상**
```
BadRequestException (Status 400: {empty JSON})
모든 DockerClientProviderStrategy 전략 실패
```

**원인**
docker-java 3.4.0은 `MAX_VERSION=1.43`을 하드코딩. Docker Desktop 29.x는 최소 API v1.44 요구.
버전 협상: `min(MAX_VERSION=1.43, server=1.53) = 1.43` → Docker Desktop이 거부.
`systemProperty 'DOCKER_API_VERSION', '1.44'`는 Java 시스템 프로퍼티를 설정하지만, docker-java는 OS 환경변수(`System.getenv()`)를 읽음 → 미동작.

**수정**
1. Testcontainers BOM 1.21.4로 업그레이드 (docker-java 신버전 포함, v1.44 지원)
2. `DOCKER_HOST` 환경변수를 `~/.docker/run/docker.sock`으로 명시 (Gradle `environment` DSL 사용)
3. `DockerDesktopApiV44Strategy`: `DefaultDockerClientConfig.withApiVersion("1.44")` 명시 (보조 수단)

```groovy
// app/build.gradle
tasks.withType(Test).configureEach {
    environment "DOCKER_HOST", "unix://${System.getProperty('user.home')}/.docker/run/docker.sock"
}
```

---

### 9. MariaDB 초기화 임시 서버로 인한 JDBC EOF

**증상**
```
java.sql.SQLNonTransientConnectionException
  Caused by: java.io.EOFException at PacketReader.java:64
```

**원인**
MariaDB Docker 이미지는 DB 초기화 시 임시 서버를 먼저 기동. 이 임시 서버가 "mariadbd: ready for connections"를 로그에 출력한 후 종료됨.
DinD 환경(bridge IP 직접 연결)에서는 임시 서버가 TCP를 바인드하지 않아 문제가 없었지만,
HostOS mapped port 방식에서는 wait 전략이 임시 서버의 로그에 반응 → 실제 TCP 포트 준비 전에 JDBC 연결 시도 → EOF.

**수정**
`Wait.forLogMessage` 단독 → `WaitAllStrategy(logMessage + forListeningPort())` 조합:

```java
.waitingFor(
    new WaitAllStrategy(WaitAllStrategy.Mode.WITH_MAXIMUM_OUTER_TIMEOUT)
        .withStrategy(Wait.forLogMessage(".*mariadbd: ready for connections.*", 1))
        .withStrategy(Wait.forListeningPort())
        .withStartupTimeout(Duration.ofSeconds(120))
)
```

`forListeningPort()`가 실제 TCP 포트가 열릴 때까지 대기 → 진짜 서버가 준비된 후에만 연결 진행.

---

### 10. OAuth2 ClientId 빈 문자열 검증 실패

**증상**
```
java.lang.IllegalStateException at OAuth2ClientProperties.java:68
  Client id must not be empty.
```

**원인**
`application.yml`의 `client-id: ${GOOGLE_CLIENT_ID:}` — 환경변수 미설정 시 빈 문자열.
테스트 프로파일 YML(`application-test.yml`, `application-test-pg.yml`)에 override 없음.
Spring Boot가 빈 clientId를 가진 OAuth2 등록을 컨텍스트 초기화 시 검증 → 예외 발생.
DinD 환경에서 통과된 이유: Flyway(MariaDB 연결)가 먼저 실패해서 OAuth2 검증 단계에 도달하지 못했기 때문.

**수정**
두 테스트 YML에 더미 OAuth2 값 추가:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-client-id
            client-secret: test-client-secret
```

---

## 디버깅 흐름 요약 (2026-02-25)

```
시도 1 (Sandbox): ./gradlew test
  → Docker 소켓 없음 → Testcontainers 불가 (예상)

시도 2 (HostOS): ./gradlew test
  → Docker Desktop 미실행 → IllegalStateException

시도 3 (HostOS, Docker 실행): ./gradlew test
  → API v1.43 거부 (Docker Desktop 29.x 요구 v1.44)

시도 4 (systemProperty 'DOCKER_API_VERSION', '1.44')
  → 미동작 (Java 시스템 프로퍼티 ≠ OS 환경변수)

시도 5 (Testcontainers BOM 1.21.4 + DOCKER_HOST 명시)
  → Docker 연결 성공, MariaDB EOF + PG OAuth2 오류

시도 6 (WaitAllStrategy + OAuth2 더미값 추가)
  → BUILD SUCCESSFUL ✓ 38/38 PASS
```

---

## 최종 성공 로그 (2026-02-25)

```
HikariPool-1 - Shutdown completed.   ← MariaDB 테스트 완료
HikariPool-2 - Shutdown completed.   ← PostgreSQL 테스트 완료

BUILD SUCCESSFUL in 29s
```

**테스트 결과**

| 테스트 클래스 | DB | 결과 |
|---|---|---|
| `AuthIntegrationTest` | MariaDB 11.2 (Testcontainers) | PASSED |
| `AuthPostgresIntegrationTest` | PostgreSQL 16.2 (Testcontainers) | PASSED |
| 단위 테스트 34개 | — | PASSED |

---

## 최종 수정 요약 (2026-02-25)

| 문제 | 원인 | 수정 |
|---|---|---|
| Docker API 버전 불일치 | docker-java `MAX_VERSION=1.43`, Docker Desktop 29.x 요구 v1.44 | Testcontainers BOM 1.21.4 업그레이드 + `DOCKER_HOST` 환경변수 명시 |
| MariaDB JDBC EOF | init 임시 서버 로그에 wait 전략이 반응, 실제 TCP 준비 전 연결 시도 | `WaitAllStrategy(logMessage + forListeningPort())` 조합으로 교체 |
| OAuth2 clientId 빈 문자열 | 테스트 YML에 override 없어 `OAuth2ClientProperties` 검증 실패 | `application-test*.yml`에 더미 OAuth2 값 추가 |
| HostOS bridge IP 접근 불가 | Mac Docker는 VM 내부 실행, bridge IP(172.17.x.x) 호스트에서 미도달 | `/.dockerenv` 존재 여부로 환경 감지 → HostOS에서 `getMappedPort()` 사용 |

---

# Debugging Log — 2026-02-24

## 환경

- OS: Linux (devcontainer, Docker-in-Docker)
- Java: OpenJDK 17.0.18
- Gradle: 8.6
- Spring Boot: 3.2.3

---

## 수정된 버그들

### 1. `api()` 빌드 실패 — `oauth2-awt-starter`

**증상**
```
Could not find method api() for arguments [project ':oauth2-awt-core']
```

**원인**
`api()` 구성은 `java-library` 플러그인이 있어야 사용 가능한데, 루트의 `apply plugin: 'java'`만 상속받아 누락됨.

**수정**
`oauth2-awt-starter/build.gradle`에 `java-library` 플러그인 추가:
```groovy
plugins {
    id 'java-library'
}
```

---

### 2. `@ConfigurationProperties` 컴파일 오류 — `fcm-client`

**증상**
```
error: package org.springframework.boot.context.properties does not exist
```

**원인**
`fcm-client`의 `FcmConfig`에서 `@ConfigurationProperties`를 사용하는데, `spring-boot-autoconfigure` 의존성이 없었음.

**수정**
`fcm-client/build.gradle`에 추가:
```groovy
compileOnly 'org.springframework.boot:spring-boot-autoconfigure'
```

---

### 3. `slf4j` 컴파일 오류 — `oauth2-awt-starter`

**증상**
```
error: package org.slf4j does not exist
```

**원인**
`Oauth2AwtAutoConfiguration`에서 Logger를 사용하는데, `slf4j-api`가 의존성에 없었음. 또한 `spring-boot-autoconfigure`가 `implementation`과 `compileOnly`에 중복 선언돼 있었음.

**수정**
`oauth2-awt-starter/build.gradle` 정리:
```groovy
compileOnly 'org.springframework.boot:spring-boot-autoconfigure'
implementation 'org.slf4j:slf4j-api'
```

---

### 4. Testcontainers 포트 연결 실패 — devcontainer DinD 환경

**증상**
```
ContainerLaunchException
  Caused by: IllegalStateException at JdbcDatabaseContainer.java:176
    Caused by: java.net.ConnectException
```

**원인**
devcontainer는 호스트 Docker 데몬의 socket(`/var/run/docker.sock`)을 마운트해서 사용하는 구조. Testcontainers가 DB 컨테이너를 시작할 때 호스트 포트 매핑(`0.0.0.0:xxxxx→3306`)을 사용하는데, 이 포트가 devcontainer 내부에서 접근 불가.

- `172.17.0.1:<mapped_port>` — 불가
- `127.0.0.1:<mapped_port>` — 불가
- `TESTCONTAINERS_HOST_OVERRIDE=172.17.0.1` — 해결 안 됨
- `MariaDBContainer` / `PostgreSQLContainer` (JdbcDatabaseContainer 서브클래스)는 wait 전략과 무관하게 내부적으로 JDBC 헬스체크를 호스트 매핑 포트로 추가 수행 → log 기반 wait 전략을 지정해도 우회 불가

**수정**
`JdbcDatabaseContainer` 대신 `GenericContainer` 사용. 컨테이너 내부 브릿지 IP(동일 Docker bridge 네트워크인 `172.17.0.x`)로 직접 연결:

```java
// MariaDbContainerSupport.java
static final GenericContainer<?> MARIADB = new GenericContainer<>(DockerImageName.parse("mariadb:11.2"))
        .withEnv("MARIADB_DATABASE", DB_NAME)
        .withEnv("MARIADB_USER", DB_USER)
        .withEnv("MARIADB_PASSWORD", DB_PASSWORD)
        .withEnv("MARIADB_ROOT_PASSWORD", "root")
        .withExposedPorts(3306)
        .waitingFor(
                Wait.forLogMessage(".*mariadbd: ready for connections.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(120))
        );

static {
    MARIADB.start();
    String ip = MARIADB.getContainerInfo()
            .getNetworkSettings().getNetworks().get("bridge").getIpAddress();
    System.setProperty("spring.datasource.url", "jdbc:mariadb://" + ip + ":3306/" + DB_NAME);
}
```

---

### 5. `IOException: 디렉터리입니다` — `googleAssertionTokenClient` 빈 생성 실패

**증상**
```
BeanCreationException: Factory method 'googleAssertionTokenClient' threw exception
  Caused by: java.io.IOException: 디렉터리입니다
    at Oauth2AwtAutoConfiguration.readServiceAccountKeyAsPem (line 71)
```

**원인**
`application.yml`에서 `service-account-key-path: ${GOOGLE_SERVICE_ACCOUNT_KEY_PATH:}` — 환경변수 미설정 시 빈 문자열 `""`로 해석됨.
`@ConditionalOnProperty`는 빈 문자열도 "속성이 존재함"으로 판단 → 조건 통과 → `Files.readAllBytes(Paths.get(""))` 실행 → 빈 경로 = 현재 디렉터리 → `IOException`.

**수정**
`@ConditionalOnProperty` → `@ConditionalOnExpression`으로 교체, 비어있지 않을 때만 활성화:

```java
// 변경 전
@ConditionalOnProperty(prefix = "oauth2.awt.google", name = "service-account-key-path")

// 변경 후
@ConditionalOnExpression("'${oauth2.awt.google.service-account-key-path:}' != ''")
```

테스트 yml(`application-test.yml`, `application-test-pg.yml`)에서 불필요한 `oauth2` 섹션도 제거.

---

### 6. 실 서비스 계정 JSON 커밋 노출

**증상**
`.secrets/google/oauth2-based-project-34e0235c144e.json`이 커밋에 포함됨.

**원인**
`.gitignore`에 `.secrets/` 디렉터리가 등록되지 않은 상태에서 `git add -A` 실행.

**수정**
```bash
# git 인덱스에서 제거 (파일 자체는 유지)
git rm --cached .secrets/google/oauth2-based-project-34e0235c144e.json

# 마지막 커밋에서 제거 (아직 push 전이므로 amend 안전)
git commit --amend --no-edit
```

`.gitignore`에 추가:
```
# Secrets directory
.secrets/
```

---

### 7. `.gitignore` Secrets 패턴 강화

**배경**
버그 6에서 `.secrets/` 추가로 즉각 대응했으나, 기존 패턴(`*-service-account.json`)이 실제 파일명(`oauth2-based-project-34e0235c144e.json`)을 커버하지 못했음. 추가적인 패턴 보완 필요.

**기존 패턴의 한계**
```
*-service-account.json   ← oauth2-based-project-34e0235c144e.json 미탐지
```

**추가된 패턴**

| 패턴 | 커버하는 파일 예시 |
|---|---|
| `.secrets/` | `.secrets/google/oauth2-based-project-xxxxx.json` |
| `*service-account*.json` | `my-service-account.json` |
| `*serviceaccount*.json` | `gcp-serviceaccount.json` |
| `*-credentials.json` | `gcp-credentials.json` |
| `*credentials*.json` | `my-credentials.json` |
| `*-key.json` | `oauth2-based-project-key.json` |
| `.env`, `.env.*` | `.env.production` |
| `application-local.yml` | 로컬 전용 설정 파일 |

**수정된 `.gitignore` Secrets 섹션**
```gitignore
# Secrets directory
.secrets/

# Key & certificate files
*.pem
*.key
*.p12
*.pfx
*.jks
*.pkcs12

# Google service account JSON
*service-account*.json
*serviceaccount*.json
*-credentials.json
*credentials*.json
*-key.json

# Environment variables
.env
.env.*
!.env.example

# Spring profile overrides with real values
application-local.yml
application-local.yaml
application-secret.yml
application-secret.yaml
```

**패턴 검증 (git check-ignore)**
```bash
$ git check-ignore -v .secrets/google/oauth2-based-project-34e0235c144e.json
.gitignore:69:.secrets/    .secrets/google/oauth2-based-project-34e0235c144e.json  ✓

$ git check-ignore -v credentials.json
.gitignore:85:*credentials*.json    credentials.json  ✓

$ git check-ignore -v service-account.json
.gitignore:82:*service-account*.json    service-account.json  ✓

$ git check-ignore -v oauth2-based-project-key.json
.gitignore:86:*-key.json    oauth2-based-project-key.json  ✓
```

---

## 디버깅 흐름 요약

```
시도 1: ./gradlew test
  → api() 빌드 실패 (java-library 누락)

시도 2: java-library 추가 후 재실행
  → @ConfigurationProperties 컴파일 오류 (spring-boot-autoconfigure 누락)

시도 3: compileOnly 추가 후 재실행
  → slf4j 컴파일 오류 (slf4j-api 누락, compileOnly 중복)

시도 4: slf4j 추가, compileOnly 정리 후 재실행
  → 컴파일 성공, 테스트 실패: JdbcDatabaseContainer 호스트 포트 JDBC 체크 실패

시도 5: TESTCONTAINERS_RYUK_DISABLED=true
  → 동일 실패 (Ryuk 문제가 아니었음)

시도 6: TESTCONTAINERS_HOST_OVERRIDE=172.17.0.1
  → 동일 실패 (매핑 포트 자체가 devcontainer에서 불가)

시도 7: MariaDBContainer → GenericContainer + log wait + 내부 IP
  → 컨테이너 기동 성공, 그러나 IOException: 디렉터리입니다

시도 8 (--info 로그 분석)
  → googleAssertionTokenClient 빈이 빈 경로로 생성 시도됨 확인

시도 9: @ConditionalOnExpression + 테스트 yml 정리
  → BUILD SUCCESSFUL ✓
```

---

## 최종 성공 로그

```
> Task :oauth2-awt-core:compileJava         UP-TO-DATE
> Task :fcm-client:compileJava              UP-TO-DATE
> Task :oauth2-awt-starter:compileJava      (재컴파일)
> Task :app:compileJava                     UP-TO-DATE
> Task :app:test

  HikariPool-1 - Shutdown completed.   ← MariaDB 테스트 완료
  HikariPool-2 - Shutdown completed.   ← PostgreSQL 테스트 완료

> Task :oauth2-awt-core:test               (단위 테스트 통과)

BUILD SUCCESSFUL in 15m 49s
14 actionable tasks: 6 executed, 8 up-to-date
```

**테스트 결과**

| 테스트 클래스 | DB | 결과 |
|---|---|---|
| `AuthIntegrationTest` | MariaDB 11.2 (Testcontainers) | PASSED |
| `AuthPostgresIntegrationTest` | PostgreSQL 16.2 (Testcontainers) | PASSED |
| `JwtAssertionBuilderTest` | — | PASSED |
| `CachedTokenProviderTest` | — | PASSED |
