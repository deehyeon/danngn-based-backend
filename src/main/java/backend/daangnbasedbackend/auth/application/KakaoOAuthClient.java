package backend.daangnbasedbackend.auth.application;

import backend.daangnbasedbackend.auth.application.dto.KakaoPublicKeys;
import backend.daangnbasedbackend.auth.application.dto.KakaoPublicKeys.KakaoPublicKey;
import backend.daangnbasedbackend.auth.application.dto.KakaoTokenRes;
import backend.daangnbasedbackend.auth.application.dto.OAuthProfile;
import backend.daangnbasedbackend.auth.application.provided.OAuthClient;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;
import backend.daangnbasedbackend.auth.exception.AuthErrorType;
import backend.daangnbasedbackend.auth.exception.AuthException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${oauth.kakao.client-id}")
    private String clientId;
    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;
    @Value("${oauth.kakao.token-uri}")
    private String tokenUri;
    @Value("${oauth.kakao.jwks-uri}")
    private String jwksUri;
    @Value("${oauth.kakao.issuer}")
    private String issuer;

    private final AtomicReference<Map<String, PublicKey>> jwksCache = new AtomicReference<>(new ConcurrentHashMap<>());

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthProfile getProfile(String code) {
        KakaoTokenRes tokenRes = exchangeCodeForToken(code);
        return parseIdToken(tokenRes.idToken());
    }

    // authorization code를 access token과 id token으로 교환
    private KakaoTokenRes exchangeCodeForToken(String code) {
        String body = "grant_type=authorization_code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&code=" + code;
        try {
            KakaoTokenRes response = postAuthorizationCodeToKakao(body);
            if (response == null || response.idToken() == null) {
                throw new AuthException(AuthErrorType.OAUTH_CODE_EXCHANGE_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            log.error("카카오 토큰 교환 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorType.OAUTH_CODE_EXCHANGE_FAILED);
        }
    }

    private KakaoTokenRes postAuthorizationCodeToKakao(String body) {
        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoTokenRes.class);
    }

    // id token을 검증하고 클레임에서 프로필 정보 추출
    private OAuthProfile parseIdToken(String idToken) {
        String kid = extractKidFromHeader(idToken);
        PublicKey publicKey = resolvePublicKey(kid);

        try {
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer(issuer)
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

            validateAudience(claims);

            String socialId = claims.getSubject();
            String nickname = claims.get("nickname", String.class);

            return new OAuthProfile(socialId, nickname, OAuthProvider.KAKAO);
        } catch (JwtException e) {
            log.warn("카카오 id_token 검증 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
    }

    // JWT 헤더에서 kid 추출
    private String extractKidFromHeader(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            Map<String, String> header = objectMapper.readValue(headerJson, new TypeReference<Map<String, String>>() {});

            return header.get("kid");
        } catch (Exception e) {
            log.error("id_token 헤더 파싱 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
    }

    // 클레임의 audience(수신자)가 clientId와 일치하는지 검증
    private void validateAudience(Claims claims) {
        Object aud = claims.get("aud");
        boolean valid = (
                aud instanceof String s && s.equals(clientId))
                || (aud instanceof Iterable<?> list
                && ((Iterable<?>) list).iterator().hasNext()
                && clientId.equals(String.valueOf(((Iterable<?>) list).iterator().next())));

        if (!valid) {
            log.warn("id_token audience 불일치: {}", aud);
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
    }

    // kid에 해당하는 공개키를 캐시에서 조회하고, 없으면 JWKS를 새로 조회하여 캐싱 후 반환
    private PublicKey resolvePublicKey(String kid) {
        PublicKey cached = jwksCache.get().get(kid);
        if (cached != null) {
            return cached;
        }

        Map<String, PublicKey> freshKeys = fetchAndCacheJwks();
        PublicKey key = freshKeys.get(kid);
        if (key == null) {
            log.error("JWKS에서 kid={} 에 해당하는 키를 찾을 수 없습니다.", kid);
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
        return key;
    }

    // JWKS 엔드포인트에서 공개키를 조회하여 캐시에 저장
    private Map<String, PublicKey> fetchAndCacheJwks() {
        try {
            KakaoPublicKeys jwks = restClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .body(KakaoPublicKeys.class);

            if (jwks == null || jwks.keys() == null) {
                throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
            }

            Map<String, PublicKey> keys = jwks.keys().stream()
                    .collect(Collectors.toMap(KakaoPublicKey::kid, k -> buildRsaPublicKey(k.n(), k.e())));

            jwksCache.set(keys);
            log.info("카카오 JWKS 갱신 완료 — 키 수: {}", keys.size());
            return keys;

        } catch (RestClientException e) {
            log.error("카카오 JWKS 조회 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
    }

    // RSA 공개키 생성
    private PublicKey buildRsaPublicKey(String n, String e) {
        try {
            BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(n));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            log.error("RSA 공개키 생성 실패: {}", ex.getMessage());
            throw new AuthException(AuthErrorType.OIDC_TOKEN_INVALID);
        }
    }
}
