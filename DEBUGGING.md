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
