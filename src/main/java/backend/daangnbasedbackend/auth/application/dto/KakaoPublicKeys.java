package backend.daangnbasedbackend.auth.application.dto;

import java.util.List;

public record KakaoPublicKeys(List<KakaoPublicKey> keys) {
    public record KakaoPublicKey(
            String kid,
            String kty,
            String alg,
            String use,
            String n,
            String e
    ) {}
}
