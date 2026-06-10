package backend.daangnbasedbackend.product.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlReq(
        @NotBlank String filename,    // 원본 파일명 (확장자 포함)
        @NotBlank String contentType  // MIME 타입 (예: image/jpeg)
) {}
