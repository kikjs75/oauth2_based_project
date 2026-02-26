# Google OAuth2 Authorization Code Flow — 전체 상세 흐름

---

## 등장인물

| 역할 | 설명 |
|------|------|
| **브라우저** | 사용자의 웹 브라우저 |
| **프론트엔드 (React :3000)** | React + Vite SPA. 사용자 UI 담당. Axios로 백엔드 호출 |
| **백엔드 (Spring :8080)** | Spring Boot. OAuth2 처리, JWT 발급, API 제공 |
| **Google (GCP)** | Google 로그인 서버. 인증 코드 및 토큰 발급 |

---

## 전체 흐름 다이어그램

```
브라우저          프론트엔드(React:3000)    백엔드(Spring:8080)        Google(GCP)
  │                      │                       │                        │
  │  ① 로그인 버튼 클릭   │                       │                        │
  │─────────────────────>│                       │                        │
  │                      │                       │                        │
  │  ② window.location   │                       │                        │
  │  = /oauth2/          │                       │                        │
  │  authorization/google│                       │                        │
  │<─────────────────────│                       │                        │
  │                      │                       │                        │
  │  ③ GET /oauth2/authorization/google          │                        │
  │──────────────────────────────────────────────>                        │
  │                      │                       │                        │
  │                      │    ④ 302 Redirect      │                        │
  │<─────────────────────────────────────────────│                        │
  │  accounts.google.com/o/oauth2/auth           │                        │
  │  ?client_id=...      │                       │                        │
  │  &redirect_uri=localhost:8080/login/oauth2/code/google                │
  │  &scope=openid+email+profile                 │                        │
  │  &state=xyz789       │                       │                        │
  │  &response_type=code │                       │                        │
  │                      │                       │                        │
  │  ⑤ Google 로그인 페이지 접속                  │                        │
  │────────────────────────────────────────────────────────────────────── >│
  │                      │                       │                        │
  │         [사용자가 Google 계정 선택 & 동의]                              │
  │                      │                       │                        │
  │                      │       ⑥ 302 Redirect  │                        │
  │<──────────────────────────────────────────────────────────────────────│
  │  localhost:8080/login/oauth2/code/google     │                        │
  │  ?code=AUTH_CODE     │  ← GCP 콘솔에 등록한 URI                        │
  │  &state=xyz789       │                       │                        │
  │                      │                       │                        │
  │  ⑦ GET /login/oauth2/code/google?code=AUTH_CODE                       │
  │──────────────────────────────────────────────>                        │
  │                      │                       │                        │
  │                      │       ⑧ POST https://oauth2.googleapis.com/token│
  │                      │                       │──────────────────────── >│
  │                      │                       │  code=AUTH_CODE        │
  │                      │                       │  client_id=...         │
  │                      │                       │  client_secret=...     │
  │                      │                       │  redirect_uri=...      │
  │                      │                       │  grant_type=           │
  │                      │                       │  authorization_code    │
  │                      │                       │                        │
  │                      │       ⑨ access_token, id_token                 │
  │                      │                       │<──────────────────────────
  │                      │                       │                        │
  │                      │       ⑩ GET https://www.googleapis.com/oauth2/v3/userinfo
  │                      │                       │──────────────────────── >│
  │                      │                       │  Authorization:        │
  │                      │                       │  Bearer access_token   │
  │                      │                       │                        │
  │                      │       ⑪ { sub, email, name, picture }          │
  │                      │                       │<──────────────────────────
  │                      │                       │                        │
  │                      │       ⑫ [GoogleOAuth2UserService]              │
  │                      │                       │  DB upsert             │
  │                      │                       │  JWT 발급              │
  │                      │                       │                        │
  │  ⑬ 302 Redirect      │                       │                        │
  │<─────────────────────────────────────────────│                        │
  │  localhost:3000/callback?token=JWT           │                        │
  │                      │                       │                        │
  │  ⑭ GET /callback?token=JWT                   │                        │
  │─────────────────────>│                       │                        │
  │                      │                       │                        │
  │                      │  [CallbackPage.jsx]   │                        │
  │                      │  localStorage         │                        │
  │                      │  .setItem("token",JWT)│                        │
  │                      │  navigate("/posts")   │                        │
  │                      │                       │                        │
  │  ⑮ 게시판 페이지 이동  │                       │                        │
  │<─────────────────────│                       │                        │
```

---

## 이후 API 호출 흐름

```
브라우저          프론트엔드(React:3000)    백엔드(Spring:8080)
  │                      │                       │
  │  게시글 목록 페이지    │                       │
  │─────────────────────>│                       │
  │                      │                       │
  │                      │  [PostListPage.jsx]   │
  │                      │  useEffect 실행        │
  │                      │  axios.get("/api/posts")
  │                      │                       │
  │                      │  [client.js 인터셉터]  │
  │                      │  localStorage         │
  │                      │  .getItem("token")    │
  │                      │  → eyJhbGci...        │
  │                      │                       │
  │                      │  GET /api/posts        │
  │                      │  Authorization:        │
  │                      │  Bearer eyJhbGci...    │
  │                      │──────────────────────>│
  │                      │                       │  [JwtAuthenticationFilter]
  │                      │                       │   JWT 서명 검증
  │                      │                       │   만료시간 확인
  │                      │                       │   SecurityContext 세팅
  │                      │                       │
  │                      │  200 OK { posts: [] } │
  │                      │<──────────────────────│
  │                      │                       │
  │  게시글 목록 렌더링    │                       │
  │<─────────────────────│                       │
```

---

## 단계별 상세 설명

### ① 로그인 버튼 클릭

사용자가 React 로그인 페이지(`LoginPage.jsx`)에서 "Google로 로그인" 버튼을 클릭합니다.

```jsx
// LoginPage.jsx
<a href="http://localhost:8080/oauth2/authorization/google">
    Google로 로그인
</a>
// 또는
window.location.href = "http://localhost:8080/oauth2/authorization/google";
```

---

### ② ~ ③ Spring Security가 Google로 리다이렉트

`/oauth2/authorization/google` 은 **프로젝트 코드 어디에도 없습니다.**
Spring Security 내부 `OAuth2AuthorizationRequestRedirectFilter` 에 하드코딩된 경로입니다.

```
기본 경로: /oauth2/authorization/{registrationId}
                                    ↑
                         application.yml의 registration 키 이름
                         (google → /oauth2/authorization/google)
```

#### registrationId 란?

`application.yml` 에서 개발자가 직접 붙인 이름입니다.

```yaml
# app/src/main/resources/application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:          ← 이게 registrationId = "google"
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid, email, profile
```

이름을 바꾸면 URL도 함께 바뀝니다.

```yaml
# registrationId를 "my-google" 로 바꾸면
registration:
  my-google:
    provider: google     ← provider 명시 필요
    client-id: ...

→ 로그인 URL: /oauth2/authorization/my-google
→ 콜백  URL: /login/oauth2/code/my-google
```

`google` 이라는 이름은 Spring Security 가 내장한 `provider.google` 설정과
**자동으로 연결**되기 때문에 관례적으로 맞춰 씁니다.

#### application.yml 과 .env 의 관계

`application.yml` 은 `${변수명:기본값}` 문법으로 `.env` 의 값을 주입받습니다.

```yaml
# application.yml — 설정 구조 정의 (git 포함)
client-id: ${GOOGLE_CLIENT_ID:}       ← .env 에서 주입
client-secret: ${GOOGLE_CLIENT_SECRET:}
```

```bash
# .env — 실제 비밀값 (git 제외)
GOOGLE_CLIENT_ID=123456.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-abc...
```

| 파일 | 역할 | git 포함 여부 |
|------|------|--------------|
| `application.yml` | 설정 구조 + 플레이스홀더 | ✅ 포함 |
| `.env.example` | 환경변수 목록 템플릿 | ✅ 포함 |
| `.env` | 실제 비밀값 | ❌ .gitignore 제외 |

Spring은 이 요청을 받으면 Google 로그인 URL을 자동 생성해 302 응답합니다.

---

### ④ Google 로그인 페이지 접속 및 동의

브라우저가 Google 로그인 페이지로 이동합니다.
사용자가 계정을 선택하고 권한에 동의하면 Google이 AUTH_CODE를 발급합니다.

---

### ⑥ AUTH_CODE란?

Google이 "이 사용자 인증 확인됨"을 증명하는 **일회용 임시 티켓**입니다.

```
# 브라우저 주소창에 잠깐 나타나는 URL
http://localhost:8080/login/oauth2/code/google
  ?code=4/0AX4XfWjV2...랜덤문자열...
  &state=xyz789

특징:
  - 유효시간: 약 10분
  - 1회용: 한 번 사용하면 즉시 폐기
  - 브라우저 URL에 잠깐 노출되지만 자체로는 위험하지 않음
    (실제 토큰으로 교환하려면 client_secret이 필요하기 때문)
```

**왜 토큰을 바로 안 주나?**

```
❌ 나쁜 방법: Google → 브라우저 URL에 access_token 직접 전달
              브라우저 히스토리, 서버 로그에 토큰 노출 → 탈취 위험

✅ 좋은 방법: Google → 브라우저에 임시 code만 전달
              Spring이 백엔드에서 code + client_secret으로 토큰 교환
              실제 토큰은 URL에 절대 노출되지 않음
```

---

### ⑦ 브라우저가 Spring에 AUTH_CODE 전달

```http
GET /login/oauth2/code/google
    ?code=4/0AX4XfWj...
    &state=xyz789
Host: localhost:8080
```

여기까지가 브라우저가 관여하는 마지막 단계입니다.
이후 ⑧ ~ ⑫ 는 Spring과 Google이 **백엔드끼리만** 통신합니다.

---

### ⑧ Spring이 Google에 code → token 교환 (백엔드끼리)

```http
POST https://oauth2.googleapis.com/token
Content-Type: application/x-www-form-urlencoded

code          = 4/0AX4XfWj...              ← 브라우저에서 받은 code
client_id     = 123456.apps.googleusercontent.com
client_secret = GOCSPX-abc...              ← GOOGLE_CLIENT_SECRET (브라우저에 절대 노출 안 됨)
redirect_uri  = http://localhost:8080/login/oauth2/code/google
grant_type    = authorization_code
```

---

### ⑨ Google이 token 발급

```json
{
  "access_token": "ya29.a0AfH6SM...",
  "id_token":     "eyJhbGci...",
  "expires_in":   3599,
  "token_type":   "Bearer"
}
```

| 토큰 | 용도 |
|------|------|
| `access_token` | Google API(userinfo 등) 호출용 |
| `id_token` | 사용자 신원 증명 JWT (Google이 서명) |

#### id_token vs 우리 앱 JWT 비교

```
┌─────────────────────┬──────────────────────────────┬──────────────────────────────┐
│                     │       Google id_token         │       우리 앱 JWT             │
├─────────────────────┼──────────────────────────────┼──────────────────────────────┤
│ 발급자              │ Google                        │ Spring (우리 서버)             │
│ 서명 키             │ Google의 RSA 비공개키          │ JWT_SECRET (우리가 생성)       │
│ 검증 방법           │ Google 공개키로 검증            │ JWT_SECRET으로 검증            │
│ 담긴 정보           │ Google 계정 정보               │ 우리 DB의 사용자 정보           │
│ 수명                │ 1시간 (Google 결정)            │ 1시간 (우리가 결정)            │
│ 사용처              │ Spring 내부에서만 사용          │ 프론트엔드 API 호출에 사용      │
│ 브라우저 노출        │ ❌ 절대 없음                   │ ✅ localStorage에 저장         │
└─────────────────────┴──────────────────────────────┴──────────────────────────────┘
```

실제 내용 비교 (JWT 디코딩 시):

```
Google id_token                          우리 앱 JWT
──────────────────────────────           ──────────────────────────────
{                                        {
  "iss": "accounts.google.com",            "sub": "42",       ← 우리 DB의 userId
  "sub": "108732456789",  ← Google ID     "username": "user@gmail.com",
  "email": "user@gmail.com",              "roles": ["ROLE_USER"],
  "name": "홍길동",                        "iat": 1700000000,
  "picture": "https://...",               "exp": 1700003600
  "iat": 1700000000,
  "exp": 1700003600
}
```

왜 id_token 을 그대로 쓰지 않나?

```
❌ id_token을 그대로 프론트에 주면
  - 우리 DB의 userId, roles 정보가 없음
  - 검증할 때마다 Google 공개키 조회 필요 (네트워크 비용)
  - ID/PW 가입 사용자와 토큰 방식이 달라짐
  - Google 서비스 장애 시 우리 API도 영향을 받음

✅ 우리 JWT를 새로 발급하면
  - 우리 DB의 userId, roles 를 담을 수 있음
  - 검증이 JWT_SECRET 하나로 로컬에서 완결
  - ID/PW 로그인, Google 로그인 모두 동일한 JWT로 통일
  - Google과 독립적으로 운영 가능
```

흐름에서의 위치:

```
Google id_token                    우리 앱 JWT
──────────────                     ──────────────────
⑨ Spring이 수신                    ⑫ Spring이 발급
   ↓                                   ↓
Spring 내부에서만 사용              ⑬ 브라우저로 전달
(사용자 정보 추출 목적)                  ↓
   ↓                               ⑭ localStorage 저장
사용 후 버림                            ↓
                                   이후 모든 API 호출에 사용
```

---

### ⑩ ~ ⑪ Spring이 사용자 정보 조회 (백엔드끼리)

```http
GET https://www.googleapis.com/oauth2/v3/userinfo
Authorization: Bearer ya29.a0AfH6SM...
```

```json
{
  "sub":     "108732456789",
  "email":   "user@gmail.com",
  "name":    "홍길동",
  "picture": "https://lh3.google.com/..."
}
```

---

### ⑫ GoogleOAuth2UserService — DB upsert 및 JWT 발급

```java
// GoogleOAuth2UserService.java

String googleId = oAuth2User.getAttribute("sub");   // Google 고유 ID
String email    = oAuth2User.getAttribute("email");

// DB에 있으면 조회, 없으면 신규 생성
User user = userRepository.findByProviderAndProviderId("google", googleId)
    .orElseGet(() -> userRepository.save(
        User.builder()
            .email(email)
            .provider("google")
            .providerId(googleId)
            .roles(Set.of("ROLE_USER"))
            .build()
    ));
```

---

### ⑬ Spring이 JWT 발급 후 프론트로 리다이렉트

```
OAuth2AuthenticationSuccessHandler.java 에서 실행

String token = jwtTokenProvider.generateToken(userId, username, roles);

redirect → http://localhost:3000/callback?token=eyJhbGci...
                                                   ↑
                                        Google 토큰이 아닌
                                        우리 앱이 자체 발급한 JWT
```

---

### ⑭ CallbackPage.jsx — JWT 저장

```jsx
// frontend/src/pages/CallbackPage.jsx

function CallbackPage() {
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const token = params.get("token");   // eyJhbGci...

        localStorage.setItem("token", token);  // 저장

        navigate("/posts");   // 게시판으로 이동
    }, []);
}
```

---

### ⑮ 이후 모든 API 호출 — Axios 인터셉터가 JWT 자동 첨부

```js
// frontend/src/api/client.js

const client = axios.create({
    baseURL: "http://localhost:8080"
});

client.interceptors.request.use(config => {
    const token = localStorage.getItem("token");  // 저장된 JWT 꺼냄
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;  // 자동 첨부
    }
    return config;
});
```

사용자가 버튼을 클릭하거나 페이지를 이동할 때마다 React 컴포넌트가 axios를 호출하고,
인터셉터가 JWT를 자동으로 붙여줍니다. 사용자는 매번 로그인할 필요가 없습니다.

---

## GCP 콘솔 설정값 정리

| 항목 | 값 | 이유 |
|------|-----|------|
| 승인된 JavaScript 원본 | (불필요) | 브라우저가 Google API를 직접 호출하지 않음 |
| **승인된 리다이렉션 URI** | `http://localhost:8080/login/oauth2/code/google` | ⑥단계에서 Google이 AUTH_CODE를 전달할 Spring 경로 |

---

## 두 가지 리다이렉트 URI 구분

```
┌─────────────────────────────────────────────────────────────────────┐
│ 1단계 리다이렉트 — GCP 콘솔에 등록                                    │
│                                                                      │
│  주체: Google → Spring                                               │
│  값  : http://localhost:8080/login/oauth2/code/google                │
│  운반: AUTH_CODE                                                     │
│  변경: 불가 (Spring Security 내부 고정 경로)                            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 2단계 리다이렉트 — OAUTH2_REDIRECT_URI 환경변수                        │
│                                                                      │
│  주체: Spring → 브라우저                                              │
│  값  : http://localhost:3000/callback  (또는 8081)                   │
│  운반: 우리 앱의 JWT                                                  │
│  변경: 가능 (application.yml 또는 환경변수로 자유롭게 설정)              │
└─────────────────────────────────────────────────────────────────────┘

Google은 2단계를 전혀 모릅니다.
1단계 URI만 GCP 콘솔에 등록하면 됩니다.
```

---

## 브라우저 관여 범위 요약

```
브라우저 관여                 백엔드끼리만
────────────────────────────┼──────────────────────────────────
① 로그인 버튼 클릭            │ ⑧ code → token 교환
③ Spring에 로그인 요청        │    (client_secret 사용)
④ Google 로그인 페이지 이동   │
⑤ Google 계정 선택 & 동의    │ ⑨ access_token, id_token 수신
⑥ AUTH_CODE 수신 (URL)      │
⑦ Spring에 AUTH_CODE 전달   │ ⑩ userinfo 조회
                             │ ⑪ 사용자 정보 수신
⑬ JWT 수신 (302 리다이렉트)  │ ⑫ DB upsert + JWT 발급
⑭ CallbackPage: JWT 저장    │
⑮ 이후 API 호출 (Bearer JWT)│

client_secret은 백엔드에만 존재
브라우저에 절대 노출되지 않음 ← 핵심 보안 포인트
```

---

## JWT 관련 환경변수 출처

| 환경변수 | 출처 | 설명 |
|----------|------|------|
| `GOOGLE_CLIENT_ID` | GCP 콘솔 → OAuth2 클라이언트 | OAuth2 앱 식별자 |
| `GOOGLE_CLIENT_SECRET` | GCP 콘솔 → OAuth2 클라이언트 | AUTH_CODE → token 교환 시 사용 |
| `JWT_SECRET` | **직접 생성** (GCP와 무관) | 우리 앱이 JWT 서명할 때 사용하는 키 |
| `OAUTH2_REDIRECT_URI` | **직접 설정** | 2단계 리다이렉트 목적지 (프론트엔드 URL) |

```bash
# JWT_SECRET 생성 방법 (256비트 랜덤)
openssl rand -base64 32
# 출력 예: K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

---

## 토큰 수명 관리

```
Google id_token                    우리 앱 JWT
──────────────────                 ──────────────────────────────
⑨ Spring이 수신                    ⑫ 발급 (exp: 지금 + 1시간)
   ↓                                   ↓
사용자 정보 추출 후 즉시 버림          localStorage 저장
(수명 의미 없음)                        ↓
                                   1시간 후 만료
                                        ↓
                                   프론트가 401 받으면
                                   → 다시 로그인 유도
```

수명 설정 위치:

```yaml
# app/src/main/resources/application.yml
app:
  jwt:
    expiration-ms: 3600000    ← 1시간 (우리가 결정)
```

Google id_token 의 수명(`expires_in: 3599`) 과는 **전혀 무관**합니다.

---

## JWT 만료 후 처리

현재 프로젝트는 **Refresh Token 없이** 단순하게 구현되어 있습니다.

```
우리 앱 JWT 만료
      ↓
API 호출 → 401 Unauthorized
      ↓
프론트엔드: localStorage의 token 삭제
      ↓
로그인 페이지로 이동
      ↓
사용자가 다시 Google 로그인
      ↓
새 JWT 발급
```

> Refresh Token 을 도입하면 만료 시 자동으로 재발급할 수 있지만,
> 현재 구조에서는 **만료 = 재로그인**입니다.
