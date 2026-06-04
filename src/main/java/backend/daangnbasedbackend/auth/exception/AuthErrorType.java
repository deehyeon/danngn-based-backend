
package backend.daangnbasedbackend.auth.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorType implements ErrorType {
    // 인증 토큰
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_FORMAT_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 형식의 토큰입니다."),

    // 회원 관련
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,     "존재하지 않는 사용자입니다."),
    UNAUTHORIZED_MEMBER_ACCESS(HttpStatus.FORBIDDEN, "권한이 없는 사용자입니다."),
    UNAUTHORIZED_SUBSCRIBE (HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),

    // OAuth / OIDC
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    OIDC_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "OIDC 토큰 검증에 실패했습니다."),
    OAUTH_CODE_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "소셜 로그인 코드 교환에 실패했습니다."),

    // 서버 에러
    JSON_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 처리 중 오류가 발생했습니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다."),
    ;

    private final HttpStatus status;

    private final String message;
}