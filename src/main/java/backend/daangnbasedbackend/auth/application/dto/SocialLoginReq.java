package backend.daangnbasedbackend.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginReq(@NotBlank String code) {}
