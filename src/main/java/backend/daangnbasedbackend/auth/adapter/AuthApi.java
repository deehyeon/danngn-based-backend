package backend.daangnbasedbackend.auth.adapter;

import backend.daangnbasedbackend.auth.application.dto.SocialLoginReq;
import backend.daangnbasedbackend.auth.application.dto.TokenRes;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "AUTH", description = "회원가입/로그인 로직 API")
@RequestMapping("/v1/auth")
public interface AuthApi {

    @Operation(summary = "카카오 소셜 로그인", description = "카카오 인가 코드를 받아 로그인 및 토큰 발급을 수행합니다.")
    @PostMapping("/kakao-social-login")
    ResponseEntity<ApiResponse<TokenRes>> kakaoSocialLogin(@RequestBody @Valid SocialLoginReq req);

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<TokenRes>> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken);

    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 수행합니다.")
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<?>> logout(AuthDetails authDetails);

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제합니다.")
    @DeleteMapping("/withdraw")
    ResponseEntity<ApiResponse<?>> withdraw(AuthDetails authDetails);
}