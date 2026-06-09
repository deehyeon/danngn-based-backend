package backend.daangnbasedbackend.product.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;

public class ProductException extends GlobalException {
    public ProductException(ErrorType errorType) {
        super(errorType);
    }
}
