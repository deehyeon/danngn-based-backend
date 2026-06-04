package backend.daangnbasedbackend.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenRes(
        @JsonProperty("token_type")               String tokenType,
        @JsonProperty("access_token")             String accessToken,
        @JsonProperty("id_token")                 String idToken,
        @JsonProperty("expires_in")               long expiresIn,
        @JsonProperty("refresh_token")            String refreshToken,
        @JsonProperty("refresh_token_expires_in") long refreshTokenExpiresIn,
        @JsonProperty("scope")                    String scope
) {}
