package backend.daangnbasedbackend.global.webapi.response;

import backend.daangnbasedbackend.global.exception.ErrorMessage;
import backend.daangnbasedbackend.global.exception.ErrorType;

public record ApiResponse<T>(ResultType result, T data, ErrorMessage error) {

    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResponse<?> error(ErrorType error) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error));
    }

    public static ApiResponse<?> error(ErrorType type, String customMessage) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(type, customMessage));
    }
}