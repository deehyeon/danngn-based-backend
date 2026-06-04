# Auth 모듈

소셜 로그인(OAuth2.0 + OIDC), JWT 발급, RTR(Refresh Token Rotation) 기반 토큰 관리, 로그아웃, 회원 탈퇴를 담당하는 인증 모듈입니다.

---

## 목차

1. [패키지 구조](#패키지-구조)
2. [인증 전체 흐름](#인증-전체-흐름)
3. [API 엔드포인트](#api-엔드포인트)
4. [카카오 소셜 로그인 (OAuth2.0 + OIDC)](#카카오-소셜-로그인-oauth20--oidc)
5. [JWT 토큰 전략](#jwt-토큰-전략)
6. [RTR (Refresh Token Rotation)](#rtr-refresh-token-rotation)
7. [보안 이슈 및 해결 방법](#보안-이슈-및-해결-방법)
8. [에러 코드](#에러-코드)
9. [환경 설정](#환경-설정)

---

## 패키지 구조

```
auth/
├── adapter/
│   ├── AuthApi.java                     # Swagger 문서 + Spring MVC 매핑 인터페이스
│   └── AuthController.java              # AuthApi 구현체 — 순수 비즈니스 로직 호출만 담당
├── application/
│   ├── AuthWriterService.java           # 인증 비즈니스 로직 구현체
│   ├── KakaoOAuthClient.java            # 카카오 OAuth2 + OIDC 클라이언트
│   ├── dto/
│   │   ├── KakaoPublicKeys.java         # 카카오 JWKS 응답 DTO
│   │   ├── KakaoTokenRes.java           # 카카오 토큰 교환 응답 DTO
│   │   ├── OAuthProfile.java            # OAuth 사용자 프로필 DTO
│   │   ├── SocialLoginReq.java          # 소셜 로그인 요청 DTO
│   │   └── TokenRes.java                # AT + RT 쌍 DTO (내부용)
│   ├── provided/
│   │   ├── AuthWriter.java              # 인증 서비스 출력 포트
│   │   └── OAuthClient.java             # OAuth 클라이언트 출력 포트
│   └── required/
│       └── AuthRepository.java          # Member 조회 입력 포트
├── domain/
│   └── OAuthProvider.java               # OAuth 제공자 열거형 (GOOGLE, KAKAO, NAVER)
└── exception/
    ├── AuthErrorType.java               # 인증 에러 코드 정의
    └── AuthException.java               # 인증 예외 클래스

# global 패키지에 위치하지만 auth와 밀접하게 연관된 클래스
global/
├── adapter/security/
│   ├── JwtAuthenticationFilter.java     # 요청별 JWT 검증 필터
│   └── JwtTokenProvider.java            # JWT 생성 및 파싱 구현체
├── application/
│   ├── TokenProviderPort.java           # JWT 포트 인터페이스
│   └── security/
│       ├── AuthDetails.java             # Spring Security UserDetails 구현
│       └── AuthDetailsService.java      # UserDetailsService 구현
```

---

## AuthApi / AuthController 분리 구조

`adapter` 레이어는 **관심사 분리** 원칙에 따라 두 파일로 역할을 나눕니다.

| 파일 | 역할 | 포함 내용 |
|------|------|-----------|
| `AuthApi` | API 계약 정의 | `@Tag`, `@Operation` (Swagger), `@RequestMapping`, `@PostMapping`, `@DeleteMapping` |
| `AuthController` | API 구현 | `AuthWriter` 호출, 쿠키 생성, `ApiResponse` 조립 |

```java
// AuthApi — 경로와 Swagger 문서만 선언
@Tag(name = "AUTH", description = "회원가입/로그인 로직 API")
@RequestMapping("/v1/auth")
public interface AuthApi {
    @Operation(summary = "카카오 소셜 로그인", ...)
    @PostMapping("/kakao-social-login")
    ResponseEntity<ApiResponse<TokenRes>> kakaoSocialLogin(@RequestBody @Valid SocialLoginReq req);
    // ...
}

// AuthController — 순수 비즈니스 로직 호출만
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthWriter authWriter;

    @Override
    public ResponseEntity<ApiResponse<TokenRes>> kakaoSocialLogin(SocialLoginReq req) {
        TokenRes tokenRes = authWriter.socialLogin(OAuthProvider.KAKAO, req.code());
        // 쿠키 설정 후 응답
    }
}
```

**장점**:
- 새 엔드포인트 추가 시 `AuthApi`에 메서드 선언 → `AuthController`에 구현만 추가
- Swagger 어노테이션이 구현 코드와 분리되어 가독성 향상
- 인터페이스 단위 테스트·모킹이 용이

---

## 인증 전체 흐름

### 소셜 로그인

```
클라이언트                                 백엔드                             카카오
    │                                     │                                │
    │ POST /v1/auth/kakao-social-login     │                                │
    │ { "code": "인가코드" }                │                                │
    │────────────────────────────────────▶│                                │
    │                                     │ POST oauth/token               │
    │                                     │────────────────────────────────▶
    │                                     │◀── { access_token, id_token } ─│
    │                                     │                                │
    │                                     │ GET /.well-known/jwks.json     │
    │                                     │────────────────────────────────▶
    │                                     │◀── { RSA 공개키 목록 } ───────    │
    │                                     │                                │
    │                                     │ id_token 서명 검증 (RS256)
    │                                     │ iss / aud / exp 클레임 검증
    │                                     │ sub(socialId) / email / nickname 추출
    │                                     │
    │                                     │ DB: oauthId + provider 조회
    │                                     │ 신규 사용자 → 자동 회원가입
    │                                     │
    │                                     │ AT 생성 (HS512, 30분)
    │                                     │ RT 생성 (HS512, 14일)
    │                                     │ Redis: RT:{memberId} = RT 저장
    │                                     │
    │◀── { accessToken }                  │
    │    Set-Cookie: refresh_token (HttpOnly, Secure, SameSite=Strict)
```

### 일반 API 요청

```
클라이언트                                  백엔드
    │                                      │
    │ GET /v1/... Authorization: Bearer AT │
    │────────────────────────────────────▶ │
    │                                      │ JwtAuthenticationFilter
    │                                      │   AT 유효 → SecurityContext 설정
    │                                      │   컨트롤러 실행
    │◀── 200 응답 ──────────────────────    │
```

### AT 만료 후 RTR 재발급

```
클라이언트                                 백엔드
    │                                     │
    │ GET /v1/... AT(만료)                 │
    │────────────────────────────────────▶│ JwtAuthenticationFilter
    │                                     │   ExpiredJwtException → TOKEN_EXPIRED
    │◀── 401 { code: "TOKEN_EXPIRED" } ── │
    │                                     │
    │ POST /v1/auth/refresh               │
    │ Cookie: refresh_token=RT            │
    │────────────────────────────────────▶│ AuthWriterService.reissueToken()
    │                                     │   ① RT JWT 파싱 → memberId
    │                                     │   ② Redis[RT:{memberId}] == RT 검증
    │                                     │   ③ 기존 RT 삭제 (RTR)
    │                                     │   ④ 새 AT + RT 발급
    │                                     │   ⑤ Redis: 새 RT 저장
    │◀── { accessToken: 새AT }            │
    │    Set-Cookie: refresh_token=새RT   │
    │                                     │
    │ GET /v1/... Authorization: 새AT     │
    │────────────────────────────────────▶│ 정상 처리
    │◀── 200 ──────────────────────────   │
```

---

## API 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/v1/auth/kakao-social-login` | 불필요 | 카카오 소셜 로그인 |
| `POST` | `/v1/auth/refresh` | 불필요 (RT 쿠키) | AT 재발급 (RTR) |
| `POST` | `/v1/auth/logout` | AT 필요 | 로그아웃 |
| `DELETE` | `/v1/auth/withdraw` | AT 필요 | 회원 탈퇴 |

### POST /v1/auth/kakao-social-login

카카오 인가 코드를 받아 로그인(최초 요청 시 자동 회원가입)합니다.

**Request**
```json
{ "code": "카카오_인가코드" }
```

**Response**
```json
{
  "result": "SUCCESS",
  "data": { "accessToken": "eyJ..." },
  "error": null
}
```
```
Set-Cookie: refresh_token=eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/v1/auth; Max-Age=1209600
```

### POST /v1/auth/refresh

RT 쿠키를 사용해 새 AT와 RT를 발급합니다. AT를 Authorization 헤더에 포함하지 않습니다.

**Request** — 별도 body 없음 (RT 쿠키 자동 전송)

**Response**
```json
{
  "result": "SUCCESS",
  "data": { "accessToken": "eyJ..." },
  "error": null
}
```
```
Set-Cookie: refresh_token=새eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/v1/auth; Max-Age=1209600
```

### POST /v1/auth/logout

Redis에서 RT를 삭제하고 클라이언트 RT 쿠키를 만료시킵니다.

**Request Header**: `Authorization: Bearer {accessToken}`

### DELETE /v1/auth/withdraw

회원을 소프트 삭제하고 세션을 종료합니다.

**Request Header**: `Authorization: Bearer {accessToken}`

---

## 카카오 소셜 로그인 (OAuth2.0 + OIDC)

### 전체 프로세스 (`KakaoOAuthClient`)

```
1. 인가 코드 → 토큰 교환
   POST https://kauth.kakao.com/oauth/token
   grant_type=authorization_code&client_id=...&redirect_uri=...&code=...
   → { access_token, id_token(JWT) }

2. JWKS 공개키 조회 및 캐싱
   GET https://kauth.kakao.com/.well-known/jwks.json
   → RSA 공개키 목록 (kid → PublicKey 매핑으로 캐싱)

3. id_token 검증
   - JWT 헤더에서 kid 추출 (Base64URL 디코딩)
   - kid로 JWKS 캐시에서 RSA 공개키 조회
   - jjwt로 RS256 서명 검증
   - iss: https://kauth.kakao.com 검증
   - aud: client_id(REST API 키) 검증
   - exp: 만료 시각 자동 검증 (jjwt)

4. 클레임에서 사용자 정보 추출
   - sub  → socialId (카카오 고유 식별자)
   - email → 이메일
   - nickname → 닉네임
```

### JWKS 캐싱 전략

```java
// AtomicReference<Map<kid, PublicKey>> 로 캐싱
// 1. 캐시에 kid가 있으면 즉시 반환
// 2. 캐시 미스 시 JWKS 재조회 (키 로테이션 대응)
// 3. 조회 후 Map 전체를 atomic하게 교체
```

### 자동 회원가입 로직

```
oauthId + provider 조합으로 기존 회원 조회
  - 존재하면 → 로그인 처리
  - 없으면 → Member.create() 호출 후 저장 (자동 가입)
```

---

## JWT 토큰 전략

### 토큰 구성

| 항목 | Access Token | Refresh Token |
|------|-------------|---------------|
| 알고리즘 | HS512 | HS512 |
| Subject | memberId | memberId |
| 만료 시간 | 30분 (기본값) | 14일 (기본값) |
| 전달 방식 | Authorization: Bearer 헤더 | HTTP-Only 쿠키 |
| 저장 위치 | 클라이언트 메모리 | 브라우저 쿠키 |
| Redis 저장 | 없음 | RT:{memberId} = RT 값 |

### RT 쿠키 속성

| 속성 | 값 | 이유 |
|------|-----|------|
| `HttpOnly` | true | JS에서 접근 불가 (XSS 방어) |
| `Secure` | true | HTTPS 전용 전송 |
| `SameSite` | Strict | 크로스 사이트 요청에서 쿠키 미전송 (CSRF 방어) |
| `Path` | /v1/auth | 인증 관련 경로에서만 쿠키 전송 |
| `Max-Age` | 14일 (초) | RT 만료 시간과 동일 |

### JwtAuthenticationFilter 제외 경로

아래 경로는 AT 검증을 건너뜁니다.

```
/swagger-ui.html, /swagger-ui/, /v1/api-docs   # API 문서
/actuator/health/readiness, /liveness          # 헬스체크
/v1/auth/kakao-social-login                    # 소셜 로그인 (AT 없음)
/v1/auth/refresh                               # RT로 처리 (만료 AT 전송 시 필터 차단 방지)
```

---

## RTR (Refresh Token Rotation)

매 재발급 시 RT를 교체하여 RT 탈취 및 재사용을 탐지합니다.

### 동작 원리

```
로그인 시:
  Redis: SET RT:{memberId} = RT_v1  (TTL = 14일)

재발급 요청 시:
  1. 클라이언트가 RT_v1 쿠키 전송
  2. Redis에서 RT:{memberId} 조회 → RT_v1
  3. 전달값 == Redis값 → 일치 확인 ✓
  4. Redis RT_v1 삭제
  5. 새 RT_v2 발급 → Redis: SET RT:{memberId} = RT_v2
  6. 새 AT + RT_v2 응답
```

### RT 탈취 탐지 시나리오

```
정상 사용자: RT_v2 보유 (재발급 후)
공격자: RT_v1 보유 (탈취한 구버전)

공격자가 RT_v1로 재발급 시도:
  1. Redis[RT:{memberId}] = RT_v2
  2. RT_v1 != RT_v2 → 불일치!
  3. Redis의 RT_v2도 즉시 삭제 → 정상 사용자 세션까지 강제 만료
  4. INVALID_REFRESH_TOKEN 예외 반환

→ 토큰 탈취 의심 상황으로 모든 세션을 종료해 피해를 최소화
```

---

## 보안 이슈 및 해결 방법

### 이슈 1: AT 만료 vs 위조 구분 불가

**문제**
필터에서 AT가 만료됐는지, 위조됐는지 구분하지 못하면 클라이언트가 부정확한 재발급 시도를 합니다.

**해결** (`JwtTokenProvider.getAuthentication`)
```java
} catch (ExpiredJwtException e) {
    throw new AuthException(AuthErrorType.TOKEN_EXPIRED);       // 401, 재발급 가능
} catch (JwtException | IllegalArgumentException e) {
    throw new AuthException(AuthErrorType.INVALID_ACCESS_TOKEN); // 401, 재발급 불가
}
```
클라이언트는 `TOKEN_EXPIRED`일 때만 `/v1/auth/refresh`를 호출합니다.

---

### 이슈 2: 필터 레벨 예외가 @RestControllerAdvice에 잡히지 않음

**문제**
`OncePerRequestFilter`에서 발생한 예외는 Spring MVC의 `@RestControllerAdvice`까지 전달되지 않아 500 오류가 반환됩니다.

**해결** (`JwtAuthenticationFilter.sendErrorResponse`)
```java
} catch (AuthException e) {
    response.setStatus(errorType.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorType)));
    return; // 필터 체인 중단
}
```
필터에서 직접 JSON 응답을 작성하고 필터 체인을 중단합니다.

---

### 이슈 3: RT를 Response Body로 반환 시 XSS 취약점

**문제**
RT를 JSON body로 반환하면 JS로 접근 가능 → XSS 공격 시 RT 탈취 가능합니다.

**해결**
- RT는 `HttpOnly` 쿠키로만 전달 (JS 접근 불가)
- AT는 메모리에 저장하고 Authorization 헤더로 전달
- 쿠키 Path를 `/v1/auth`로 제한해 불필요한 경로에서 쿠키 미전송

---

### 이슈 4: RT 재사용 공격 (Replay Attack)

**문제**
한번 사용된 RT가 무효화되지 않으면 탈취된 RT로 무한 재발급이 가능합니다.

**해결 (RTR)**
재발급 시 무조건 RT 교체. Redis 불일치 시 해당 세션 전체 강제 만료.
[RTR 섹션](#rtr-refresh-token-rotation) 참조.

---

### 이슈 5: CSRF 공격

**문제**
공격자의 사이트에서 사용자의 브라우저를 통해 `/v1/auth/refresh`, `/v1/auth/logout` 등을 호출할 수 있습니다.

**해결**
RT 쿠키에 `SameSite=Strict` 설정. 크로스 사이트 요청에서는 쿠키가 전송되지 않습니다.

> **주의**: 프론트엔드와 백엔드가 다른 도메인일 경우 `SameSite=None; Secure`으로 변경하고 CORS를 명시적으로 제한해야 합니다.

---

### 이슈 6: OIDC id_token 위조 방지

**문제**
공격자가 임의로 만든 id_token으로 다른 사용자로 로그인을 시도할 수 있습니다.

**해결** (`KakaoOAuthClient`)
```
1. 카카오 JWKS에서 RSA 공개키 조회 (kid 매핑)
2. RS256 서명 검증 → 카카오의 개인키로 서명된 토큰만 통과
3. iss: https://kauth.kakao.com 검증 (카카오 발급 토큰만 허용)
4. aud: client_id 검증 (우리 앱용 토큰만 허용)
5. exp: 만료 시각 자동 검증
```

---

### 이슈 7: 탈퇴 회원의 토큰 재사용

**문제**
회원 탈퇴 후에도 기존 RT로 재발급을 시도할 수 있습니다.

**해결**
- 탈퇴 시 `member.softDelete()` + `Redis RT 즉시 삭제`
- `reissueToken` 내에서 `isDeleted` 필드를 체크해 탈퇴 회원은 재발급 불가

```java
authRepository.findById(memberId)
    .filter(m -> !m.getIsDeleted())  // 탈퇴 회원 차단
    .orElseThrow(() -> new AuthException(AuthErrorType.MEMBER_NOT_FOUND));
```

---

### 이슈 8: 만료된 AT로 /auth/refresh 호출 시 필터 차단

**문제**
일부 HTTP 클라이언트(axios 인터셉터 등)는 모든 요청에 저장된 AT를 자동으로 삽입합니다.
`/v1/auth/refresh` 호출 시 만료된 AT가 헤더에 담기면, 필터가 이를 감지해 refresh 엔드포인트 도달 전에 401을 반환합니다.

**해결**
`JwtAuthenticationFilter.EXCLUDE_URLS`에 `/v1/auth/refresh`를 추가합니다.
```java
"/v1/auth/refresh"  // AT가 만료된 상태로 호출되므로 필터 우회 필수
```

---

## 에러 코드

| 코드 | HTTP Status | 메시지 | 발생 상황 |
|------|-------------|--------|-----------|
| `INVALID_ACCESS_TOKEN` | 401 | 유효하지 않은 토큰입니다. | AT 서명 위조 또는 형식 오류 |
| `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다. | AT 만료 → `/v1/auth/refresh` 호출 신호 |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 리프레시 토큰입니다. | RT 만료, 위조, 재사용 시도 |
| `INVALID_FORMAT_TOKEN` | 400 | 잘못된 형식의 토큰입니다. | JWT 구조 자체가 잘못된 경우 |
| `MEMBER_NOT_FOUND` | 404 | 존재하지 않는 사용자입니다. | 탈퇴 회원 또는 DB 불일치 |
| `UNAUTHORIZED_MEMBER_ACCESS` | 403 | 권한이 없는 사용자입니다. | 타인 리소스 접근 시도 |
| `UNSUPPORTED_PROVIDER` | 400 | 지원하지 않는 소셜 로그인 제공자입니다. | GOOGLE, NAVER 등 미구현 provider |
| `OIDC_TOKEN_INVALID` | 401 | OIDC 토큰 검증에 실패했습니다. | id_token 서명·클레임 검증 실패 |
| `OAUTH_CODE_EXCHANGE_FAILED` | 400 | 소셜 로그인 코드 교환에 실패했습니다. | 인가 코드 만료 또는 카카오 오류 |

---

## 환경 설정

`application.properties` (또는 환경변수)에 아래 설정이 필요합니다.

```properties
# JWT
jwt.secret=${JWT_SECRET}                           # 32자 이상 랜덤 문자열
jwt.token.access-expiration-time=1800000           # 30분 (ms)
jwt.token.refresh-expiration-time=1209600000       # 14일 (ms)

# Kakao OAuth2 + OIDC
oauth.kakao.client-id=${KAKAO_CLIENT_ID}           # 카카오 REST API 키
oauth.kakao.redirect-uri=${KAKAO_REDIRECT_URI}     # 카카오 개발자 콘솔에 등록된 URI
oauth.kakao.token-uri=https://kauth.kakao.com/oauth/token
oauth.kakao.jwks-uri=https://kauth.kakao.com/.well-known/jwks.json
oauth.kakao.issuer=https://kauth.kakao.com
```

### 카카오 개발자 콘솔 설정 필수 항목

- **OpenID Connect 활성화**: 앱 설정 → 카카오 로그인 → OpenID Connect 활성화
- **동의항목**: `profile_nickname`, `account_email` 필수 동의 설정
- **Redirect URI 등록**: `${KAKAO_REDIRECT_URI}` 값을 카카오 콘솔에 등록

### 로컬 개발 환경 주의사항

RT 쿠키의 `Secure=true` 설정으로 인해 HTTPS가 없는 로컬에서는 쿠키가 전송되지 않습니다.
아래 중 하나를 선택합니다.

1. **mkcert**로 로컬 HTTPS 환경 구성 (권장)
2. `application-local.properties`에서 `secure(false)` 오버라이드
