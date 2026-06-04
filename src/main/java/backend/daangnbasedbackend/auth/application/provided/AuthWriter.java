package backend.daangnbasedbackend.auth.application.provided;

import backend.daangnbasedbackend.auth.application.dto.TokenRes;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;

public interface AuthWriter {
    TokenRes socialLogin(OAuthProvider provider, String code);
    TokenRes reissueToken(String refreshToken);
    void logout(Long memberId);
    void withdraw(Long memberId);
}
