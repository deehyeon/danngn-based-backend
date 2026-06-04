package backend.daangnbasedbackend.global.exception.common;

import backend.daangnbasedbackend.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorType implements ErrorType {
    //Redis
    REDIS_SET_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis에 값을 저장하는 데 실패했습니다."),
    REDIS_GET_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis에서 값을 가져오는 데 실패했습니다."),
    REDIS_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis에서 값을 삭제하는 데 실패했습니다."),
    SHA256_GENERATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SHA256 해시 생성에 실패했습니다."),

    // Common Errors
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 내부 오류입니다."),
    FAILED_REQUEST_VALIDATION(HttpStatus.BAD_REQUEST, "요청 데이터 검증에 실패하였습니다."),
    INVALID_REQUEST_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청 인자입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패하였습니다."),

    //Email
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일은 필수입니다."),
    EMAIL_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 아닙니다."),

    //Conditional Request / Concurrency (ETag / If-Match)
    PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "If-Match 헤더가 필요합니다."),
    PRECONDITION_FAILED(HttpStatus.PRECONDITION_FAILED, "리소스 버전이 일치하지 않습니다. 새로고침 후 다시 시도하세요."),

    // 컬렉션 델타 검증
    DUPLICATED_CHILD_ID(HttpStatus.BAD_REQUEST, "요청에 중복된 자식 ID가 포함되어 있습니다."),
    INVALID_CHILD_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 자식 ID입니다."),

    // Storage
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");

    private final HttpStatus status;

    private final String message;
}
