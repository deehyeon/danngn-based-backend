package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.application.provided.StoragePort;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 로컬 개발 환경 전용 파일 업로드 엔드포인트.
 * storage.type=local(기본값)일 때만 활성화된다.
 *
 * 클라이언트 흐름:
 *   POST /v1/storage/upload (파일 첨부) → { fileUrl } 반환 → 상품 등록/수정 시 fileUrl 전달
 */
@Tag(name = "LOCAL STORAGE", description = "로컬 스토리지 직접 업로드 (개발 환경 전용)")
@RestController
@RequestMapping("/v1/storage")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageController {

    private final StoragePort storagePort;

    @Operation(
            summary = "로컬 파일 직접 업로드 (개발 환경 전용)",
            description = "파일을 백엔드 로컬 디스크에 직접 저장하고 접근 URL을 반환한다. 운영(S3) 환경에서는 /v1/storage/presigned-url을 사용한다."
    )
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "products") String directory) throws IOException {

        String fileUrl = storagePort.upload(
                directory,
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
        );
        return ApiResponse.success(fileUrl);
    }
}
