package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.application.dto.PresignedUrlRes;
import backend.daangnbasedbackend.global.application.provided.StoragePort;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.product.application.dto.PresignedUrlReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라우드 스토리지(S3 등) 환경 전용 Presigned URL 발급 엔드포인트.
 * storage.type=local 환경에서는 비활성화되고 LocalStorageController를 사용한다.
 *
 * 클라이언트 흐름:
 *   POST /v1/storage/presigned-url → { presignedUrl, fileUrl }
 *   → 클라이언트가 presignedUrl에 직접 파일 업로드
 *   → fileUrl을 상품 등록/수정 시 백엔드에 전달
 */
@Tag(name = "STORAGE", description = "클라우드 스토리지 Presigned URL API")
@RestController
@RequestMapping("/v1/storage")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class StorageController {
    private final StoragePort storagePort;

    @Operation(
            summary = "이미지 업로드용 Presigned URL 발급",
            description = "프론트엔드가 반환된 presignedUrl로 클라우드에 직접 업로드하고, fileUrl을 상품 등록/수정 시 전달한다."
    )
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlRes> getPresignedUrl(
            @AuthenticationPrincipal AuthDetails authDetails,
            @RequestBody @Valid PresignedUrlReq req) {
        return ApiResponse.success(
                storagePort.generatePresignedUrl("products", req.filename(), req.contentType())
        );
    }
}
