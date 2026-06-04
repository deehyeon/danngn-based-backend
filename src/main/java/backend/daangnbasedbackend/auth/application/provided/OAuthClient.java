package backend.daangnbasedbackend.auth.application.provided;

import backend.daangnbasedbackend.auth.application.dto.OAuthProfile;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;

public interface OAuthClient {
    OAuthProvider getProvider();
    OAuthProfile getProfile(String code);
}
