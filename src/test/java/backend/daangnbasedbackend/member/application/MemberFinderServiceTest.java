package backend.daangnbasedbackend.member.application;

import backend.daangnbasedbackend.member.application.dto.MemberRes;
import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.domain.Member;
import backend.daangnbasedbackend.member.domain.MemberRole;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberFinderServiceTest {

    @Mock private MemberRepository memberRepository;

    private MemberFinder memberFinder;

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        memberFinder = new MemberFinderService(memberRepository);
    }

    // ==========================================
    // findById
    // ==========================================

    @Test
    @DisplayName("ID로 회원 조회: 회원 정보가 MemberRes로 반환된다")
    void findById_returnsMemberRes() {
        // given
        Member member = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        // when
        MemberRes result = memberFinder.findById(MEMBER_ID);

        // then
        assertThat(result.nickname()).isEqualTo("닉네임");
        assertThat(result.role()).isEqualTo(MemberRole.GUEST);
        assertThat(result.mannerTemp()).isEqualTo(36.5);
    }

    @Test
    @DisplayName("ID로 회원 조회 — 존재하지 않는 ID: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void findById_notFound_throwsException() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberFinder.findById(MEMBER_ID))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }

    // ==========================================
    // findByEmail
    // ==========================================

    @Test
    @DisplayName("이메일로 회원 조회: 회원 정보가 MemberRes로 반환된다")
    void findByEmail_returnsMemberRes() {
        // given
        Member member = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        member.registerAdditionalInfo("서울 강남구", EMAIL, "010-1234-5678");
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

        // when
        MemberRes result = memberFinder.findByEmail(EMAIL);

        // then
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.location()).isEqualTo("서울 강남구");
        assertThat(result.role()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("이메일로 회원 조회 — 존재하지 않는 이메일: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void findByEmail_notFound_throwsException() {
        // given
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberFinder.findByEmail(EMAIL))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }
}
