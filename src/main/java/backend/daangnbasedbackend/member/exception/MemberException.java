package backend.daangnbasedbackend.member.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;

public class MemberException extends GlobalException {

    public MemberException(ErrorType errorType) {
        super(errorType);
    }
}
