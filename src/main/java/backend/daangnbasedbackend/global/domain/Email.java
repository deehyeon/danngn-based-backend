package backend.daangnbasedbackend.global.domain;

import backend.daangnbasedbackend.auth.exception.AuthErrorType;
import backend.daangnbasedbackend.auth.exception.AuthException;
import backend.daangnbasedbackend.global.exception.common.GlobalErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;

import java.util.regex.Pattern;

/**
 * 이메일 값 객체
 */
public record Email(String address) {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    public Email {
        if (!EMAIL_PATTERN.matcher(address).matches()) {
            throw new GlobalException(GlobalErrorType.EMAIL_INVALID_FORMAT);
        }
    }
    public static Email from(String email) {
        return new Email(email);
    }
}