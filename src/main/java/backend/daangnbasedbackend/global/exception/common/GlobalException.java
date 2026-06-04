package backend.daangnbasedbackend.global.exception.common;

import backend.daangnbasedbackend.global.exception.ErrorType;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {
    private final ErrorType errorType;

    public GlobalException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }
}