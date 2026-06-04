package backend.daangnbasedbackend.auth.application.dto;

public record TokenRes(
        String accessToken,
        String refreshToken
) { }
