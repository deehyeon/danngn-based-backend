package backend.daangnbasedbackend.member.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorType implements ErrorType {
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,     "존재하지 않는 사용자입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "올바르지 않은 입력값입니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주소입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호입니다."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "유효하지 않은 이름입니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "닉네임은 20자 이하여야 합니다."),
    LOCATION_NOT_SET(HttpStatus.BAD_REQUEST, "동네 설정이 필요합니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
