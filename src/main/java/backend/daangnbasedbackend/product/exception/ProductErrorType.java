package backend.daangnbasedbackend.product.exception;

import backend.daangnbasedbackend.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorType implements ErrorType {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    INVALID_TRADE_REQUEST(HttpStatus.BAD_REQUEST, "올바르지 않은 요청입니다."),
    INVALID_STATE_MODIFICATION(HttpStatus.BAD_REQUEST, "현재 예약 중이 아닙니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "해당 상품에 대한 권한이 없습니다."),
    REFRESH_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "끌어올리기는 최대 5회까지 가능합니다."),
    REFRESH_COOLDOWN_NOT_ELAPSED(HttpStatus.BAD_REQUEST, "이전 끌어올리기 이후 24시간이 지나야 합니다.");

    private final HttpStatus status;
    private final String message;
}
