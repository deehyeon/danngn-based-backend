package backend.daangnbasedbackend.auth.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;

public class AuthException extends GlobalException {
    public AuthException(ErrorType errorType) {
        super(errorType);
    }
}
