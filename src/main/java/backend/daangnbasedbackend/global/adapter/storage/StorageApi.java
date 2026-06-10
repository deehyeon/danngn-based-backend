package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.application.dto.PresignedUrlRes;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.product.application.dto.PresignedUrlReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "STORAGE", description = "클라우드 스토리지 Presigned URL API")
@RequestMapping("/v1/storage")
public interface StorageApi {

    @Operation(
            summary = "이미지 업로드용 Presigned URL 발급",
            description = "프론트엔드가 반환된 presignedUrl로 클라우드에 직접 업로드하고, fileUrl을 상품 등록/수정 시 전달한다."
    )
    @PostMapping("/presigned-url")
    ApiResponse<PresignedUrlRes> getPresignedUrl(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid PresignedUrlReq req);
}
