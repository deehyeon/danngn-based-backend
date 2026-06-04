package backend.daangnbasedbackend.auth.integration;

import backend.daangnbasedbackend.auth.application.KakaoOAuthClient;
import backend.daangnbasedbackend.auth.application.dto.OAuthProfile;
import backend.daangnbasedbackend.auth.domain.OAuthProvider;
import backend.daangnbasedbackend.global.application.MemoryMap;
import backend.daangnbasedbackend.global.application.TokenProviderPort;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;

    @MockitoBean KakaoOAuthClient kakaoOAuthClient;
    @MockitoBean MemoryMap memoryMap;

    private static final String BASE_URL = "/v1/auth";

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("신규 회원 소셜 로그인: DB에 회원이 저장되고 AT가 응답된다")
    void newMember_loginCreatesDbRecordAndReturnsToken() throws Exception {
        // given
        OAuthProfile profile = new OAuthProfile("new-social-id", "테스트유저", OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProvider()).willReturn(OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProfile(anyString())).willReturn(profile);

        // when
        mockMvc.perform(post(BASE_URL + "/kakao-social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "kakao-auth-code"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));

        // then: DB에 회원이 저장됨
        assertThat(memberRepository.findByOauthIdAndProvider("new-social-id", "KAKAO")).isPresent();
    }

    @Test
    @DisplayName("동일 소셜 계정 재로그인: 회원이 중복 생성되지 않는다")
    void existingMember_loginDoesNotDuplicate() throws Exception {
        // given
        OAuthProfile profile = new OAuthProfile("existing-social-id", "테스트유저", OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProvider()).willReturn(OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProfile(anyString())).willReturn(profile);

        String requestBody = objectMapper.writeValueAsString(Map.of("code", "auth-code"));

        // when: 같은 계정으로 두 번 로그인
        mockMvc.perform(post(BASE_URL + "/kakao-social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + "/kakao-social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // then: DB에 회원이 1명만 존재함
        long count = memberRepository.findAll().stream()
                .filter(m -> "existing-social-id".equals(m.getOauthId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("토큰 재발급: 유효한 RT로 새 AT를 발급받는다")
    void reissueToken_withValidRt_returnsNewAt() throws Exception {
        // given: 로그인하여 RT 획득
        OAuthProfile profile = new OAuthProfile("rt-test-social-id", "유저", OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProvider()).willReturn(OAuthProvider.KAKAO);
        given(kakaoOAuthClient.getProfile(anyString())).willReturn(profile);

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/kakao-social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "code"))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = loginResult.getResponse().getCookie("refresh_token").getValue();
        String firstAccessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Redis mock: 저장된 RT 반환
        given(memoryMap.getValue(anyString())).willReturn(refreshToken);

        Thread.sleep(1000);

        // when
        MvcResult refreshResult = mockMvc.perform(post(BASE_URL + "/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // then: AT가 새로 발급됨 (RTR)
        assertThat(newAccessToken).isNotEqualTo(firstAccessToken);
    }
}
