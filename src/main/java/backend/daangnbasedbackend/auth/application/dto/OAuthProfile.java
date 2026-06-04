package backend.daangnbasedbackend.auth.application.dto;

import backend.daangnbasedbackend.auth.domain.OAuthProvider;

public record OAuthProfile(
        String socialId,
        String nickname,
        OAuthProvider provider
) {}
