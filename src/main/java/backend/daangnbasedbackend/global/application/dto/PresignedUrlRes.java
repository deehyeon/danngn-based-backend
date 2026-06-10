package backend.daangnbasedbackend.global.application.dto;

public record PresignedUrlRes(
        String presignedUrl,
        String fileUrl
) {}
