package backend.daangnbasedbackend.auth.application;

import backend.daangnbasedbackend.auth.application.dto.OAuthProfile;
import backend.daangnbasedbackend.auth.application.dto.TokenRes;
import backend.daangnbasedbackend.auth.application.provided.AuthWriter;
import backend.daangnbasedbackend.auth.application.provided.OAuthClient;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;
import backend.daangnbasedbackend.auth.exception.AuthErrorType;
import backend.daangnbasedbackend.auth.exception.AuthException;
import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import backend.daangnbasedbackend.global.application.provided.TokenProviderPort;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.domain.Member;
import backend.daangnbasedbackend.member.exception.MemberException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthWriterServiceTest {
    @Mock private OAuthClient kakaoOAuthClient;
    @Mock private MemoryMap memoryMap;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private MemberRepository memberRepository;

    private AuthWriter authWriter;

    private static final String RT_PREFIX = "RT:";
    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";

    @BeforeEach
    void setUp() {
        when(kakaoOAuthClient.getProvider()).thenReturn(OAuthProvider.KAKAO);
        authWriter = new AuthWriterService(
                List.of(kakaoOAuthClient), memoryMap, tokenProviderPort, memberRepository
        );
    }

    @Test
    @DisplayName("신규 회원: 카카오 프로필로 GUEST 회원을 생성하고 토큰을 발급한다")
    void createGuestAndIssueTokens() {
        OAuthProfile profile = new OAuthProfile("social-id-1", "닉네임", OAuthProvider.KAKAO);
        Member savedMember = Member.createGuest("닉네임", "social-id-1", "KAKAO");

        when(kakaoOAuthClient.getProfile("auth-code")).thenReturn(profile);
        when(memberRepository.findByOauthIdAndProvider("social-id-1", "KAKAO")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);
        when(tokenProviderPort.createAccessToken(any())).thenReturn(ACCESS_TOKEN);
        when(tokenProviderPort.createRefreshToken(any())).thenReturn(REFRESH_TOKEN);
        when(tokenProviderPort.getRefreshTokenExpiration()).thenReturn(1209600000L);

        TokenRes result = authWriter.socialLogin(OAuthProvider.KAKAO, "auth-code");

        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("기존 회원: DB에서 회원을 조회하고 신규 생성 없이 토큰을 발급한다")
    void findAndIssueTokens() {
        OAuthProfile profile = new OAuthProfile("social-id-1", "닉네임", OAuthProvider.KAKAO);
        Member existingMember = Member.createGuest("닉네임", "social-id-1", "KAKAO");

        when(kakaoOAuthClient.getProfile("auth-code")).thenReturn(profile);
        when(memberRepository.findByOauthIdAndProvider("social-id-1", "KAKAO")).thenReturn(Optional.of(existingMember));
        when(tokenProviderPort.createAccessToken(any())).thenReturn(ACCESS_TOKEN);
        when(tokenProviderPort.createRefreshToken(any())).thenReturn(REFRESH_TOKEN);
        when(tokenProviderPort.getRefreshTokenExpiration()).thenReturn(1209600000L);

        TokenRes result = authWriter.socialLogin(OAuthProvider.KAKAO, "auth-code");

        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("지원하지 않는 provider: AuthException(UNSUPPORTED_PROVIDER)을 던진다")
    void unsupportedProviderThrowsException() {
        assertThatThrownBy(() -> authWriter.socialLogin(OAuthProvider.GOOGLE, "code"))
                .isInstanceOf(AuthException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.UNSUPPORTED_PROVIDER);
    }

    @Test
    @DisplayName("유효한 RT: 기존 RT를 삭제하고 새 AT/RT를 발급한다 (RTR)")
    void validRefreshTokenRotatesAndIssuesNewTokens() {
        when(tokenProviderPort.parseRefreshToken(REFRESH_TOKEN)).thenReturn(MEMBER_ID);
        when(memoryMap.getValue(RT_PREFIX + MEMBER_ID)).thenReturn(REFRESH_TOKEN);
        when(tokenProviderPort.createAccessToken(MEMBER_ID)).thenReturn("new-" + ACCESS_TOKEN);
        when(tokenProviderPort.createRefreshToken(MEMBER_ID)).thenReturn("new-" + REFRESH_TOKEN);
        when(tokenProviderPort.getRefreshTokenExpiration()).thenReturn(1209600000L);

        TokenRes result = authWriter.reissueToken(REFRESH_TOKEN);

        assertThat(result.accessToken()).isEqualTo("new-" + ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo("new-" + REFRESH_TOKEN);
        verify(memoryMap).deleteValue(RT_PREFIX + MEMBER_ID);
        verify(memoryMap).setValue(eq(RT_PREFIX + MEMBER_ID), eq("new-" + REFRESH_TOKEN), anyLong());
    }

    @Test
    @DisplayName("null 또는 빈 문자열 RT: AuthException(INVALID_REFRESH_TOKEN)을 던진다")
    void invalidRefreshTokenThrowsException() {
        assertThatThrownBy(() -> authWriter.reissueToken(null))
                .isInstanceOf(AuthException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_REFRESH_TOKEN);

        assertThatThrownBy(() -> authWriter.reissueToken("   "))
                .isInstanceOf(AuthException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("RT 불일치(재사용 의심): Redis에서 RT를 삭제하고 AuthException을 던진다")
    void mismatchedRefreshTokenDeletesStoredAndThrows() {
        when(tokenProviderPort.parseRefreshToken("old-rt")).thenReturn(MEMBER_ID);
        when(memoryMap.getValue(RT_PREFIX + MEMBER_ID)).thenReturn("current-rt-in-redis");

        assertThatThrownBy(() -> authWriter.reissueToken("old-rt"))
                .isInstanceOf(AuthException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.INVALID_REFRESH_TOKEN);

        verify(memoryMap).deleteValue(RT_PREFIX + MEMBER_ID);
    }

    @Test
    @DisplayName("로그아웃: Redis에서 해당 회원의 RT를 삭제한다")
    void logoutDeletesRefreshTokenFromRedis() {
        authWriter.logout(MEMBER_ID);
        verify(memoryMap).deleteValue(RT_PREFIX + MEMBER_ID);
    }

    @Test
    @DisplayName("회원 탈퇴: softDelete 처리 후 Redis RT를 삭제한다")
    void withdrawSoftDeletesAndRemovesToken() {
        Member member = Member.createGuest("닉네임", "social-id-1", "KAKAO");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        authWriter.withdraw(MEMBER_ID);

        assertThat(member.getIsDeleted()).isTrue();
        verify(memoryMap).deleteValue(RT_PREFIX + MEMBER_ID);
    }

    @Test
    @DisplayName("존재하지 않거나 이미 탈퇴한 회원: MemberException을 던진다")
    void withdrawInvalidMemberThrowsException() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authWriter.withdraw(MEMBER_ID))
                .isInstanceOf(MemberException.class);

        Member deletedMember = Member.createGuest("닉네임", "social-id-1", "KAKAO");
        deletedMember.softDelete();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(deletedMember));

        assertThatThrownBy(() -> authWriter.withdraw(MEMBER_ID))
                .isInstanceOf(MemberException.class);
    }
}