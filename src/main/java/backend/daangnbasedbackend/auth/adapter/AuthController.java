package backend.daangnbasedbackend.auth.adapter;

import backend.daangnbasedbackend.auth.application.dto.SocialLoginReq;
import backend.daangnbasedbackend.auth.application.dto.TokenRes;
import backend.daangnbasedbackend.auth.application.provided.AuthWriter;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthWriter authWriter;

    @Value("${jwt.token.refresh-expiration-time}")
    private long refreshTokenExpirationMs;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/v1/auth";

    @Override
    public ResponseEntity<ApiResponse<TokenRes>> kakaoSocialLogin(SocialLoginReq req) {
        TokenRes tokenRes = authWriter.socialLogin(OAuthProvider.KAKAO, req.code());
        ResponseCookie cookie = buildRefreshCookie(tokenRes.refreshToken(), refreshTokenExpirationMs / 1000);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(tokenRes));
    }

    @Override
    public ResponseEntity<ApiResponse<TokenRes>> refresh(String refreshToken) {
        TokenRes newTokens = authWriter.reissueToken(refreshToken);
        ResponseCookie cookie = buildRefreshCookie(newTokens.refreshToken(), refreshTokenExpirationMs / 1000);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(newTokens));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> logout(@AuthenticationPrincipal AuthDetails authDetails) {
        authWriter.logout(authDetails.getMemberId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
                .body(ApiResponse.success());
    }

    @Override
    public ResponseEntity<ApiResponse<?>> withdraw(@AuthenticationPrincipal AuthDetails authDetails) {
        authWriter.withdraw(authDetails.getMemberId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
                .body(ApiResponse.success());
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
