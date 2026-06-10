package backend.daangnbasedbackend.auth.application;

import backend.daangnbasedbackend.auth.application.dto.OAuthProfile;
import backend.daangnbasedbackend.auth.application.dto.TokenRes;
import backend.daangnbasedbackend.auth.application.provided.AuthWriter;
import backend.daangnbasedbackend.auth.application.provided.OAuthClient;
import backend.daangnbasedbackend.global.application.provided.TokenProviderPort;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;
import backend.daangnbasedbackend.auth.exception.AuthErrorType;
import backend.daangnbasedbackend.auth.exception.AuthException;
import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.domain.Member;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthWriterService implements AuthWriter {
    private final List<OAuthClient> oAuthClients;
    private final MemoryMap memoryMap;
    private final TokenProviderPort tokenProviderPort;

    private static final String RT_PREFIX = "RT:";
    private final MemberRepository memberRepository;

    @Override
    public TokenRes socialLogin(OAuthProvider provider, String code) {
        OAuthClient client = getOAuthClient(provider);
        OAuthProfile profile = client.getProfile(code);

        Member member = memberRepository.findByOauthIdAndProvider(profile.socialId(), profile.provider().name())
                .orElseGet(() -> memberRepository.save(Member.createGuest(profile.nickname(), profile.socialId(), profile.provider().name())));

        return issueTokens(member.getId());
    }

    @Override
    public TokenRes reissueToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorType.INVALID_REFRESH_TOKEN);
        }

        Long memberId = tokenProviderPort.parseRefreshToken(refreshToken);
        String storedRt = memoryMap.getValue(RT_PREFIX + memberId);

        if (!refreshToken.equals(storedRt)) {
            memoryMap.deleteValue(RT_PREFIX + memberId);
            log.warn("RT 불일치 감지 (재사용 의심) — memberId: {}", memberId);
            throw new AuthException(AuthErrorType.INVALID_REFRESH_TOKEN);
        }

        memoryMap.deleteValue(RT_PREFIX + memberId);
        log.info("RTR 재발급 완료 — memberId: {}", memberId);
        return issueTokens(memberId);
    }

    @Override
    public void logout(Long memberId) {
        memoryMap.deleteValue(RT_PREFIX + memberId);
        log.info("로그아웃 처리 완료 — memberId: {}", memberId);
    }

    @Override
    public void withdraw(Long memberId) {
        Member member = findMemberById(memberId);
        member.softDelete();
        memoryMap.deleteValue(RT_PREFIX + memberId);
        log.info("회원 탈퇴 처리 완료 — memberId: {}", memberId);
    }

    private OAuthClient getOAuthClient(OAuthProvider provider) {
        return oAuthClients.stream()
                .filter(c -> c.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new AuthException(AuthErrorType.UNSUPPORTED_PROVIDER));
    }

    private TokenRes issueTokens(Long memberId) {
        String accessToken  = tokenProviderPort.createAccessToken(memberId);
        String refreshToken = tokenProviderPort.createRefreshToken(memberId);
        memoryMap.setValue(RT_PREFIX + memberId, refreshToken, tokenProviderPort.getRefreshTokenExpiration());
        return new TokenRes(accessToken, refreshToken);
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new MemberException(MemberErrorType.MEMBER_NOT_FOUND));
    }
}
