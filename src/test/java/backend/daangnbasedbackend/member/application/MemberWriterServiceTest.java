package backend.daangnbasedbackend.member.application;

import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;
import backend.daangnbasedbackend.member.application.provided.MemberWriter;
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
class MemberWriterServiceTest {

    @Mock private MemberRepository memberRepository;

    private MemberWriter memberWriter;

    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        memberWriter = new MemberWriterService(memberRepository);
    }

    // ==========================================
    // registerAdditionalInfo
    // ==========================================

    @Test
    @DisplayName("추가 정보 등록: 위치·이메일·전화번호가 저장되고 역할이 USER로 승격된다")
    void registerAdditionalInfo_savesInfoAndUpgradesRole() {
        // given
        Member member = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        MemberInfoReq req = new MemberInfoReq("test@example.com", "서울 강남구", "010-1234-5678");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        // when
        memberWriter.registerAdditionalInfo(MEMBER_ID, req);

        // then
        assertThat(member.getEmail()).isEqualTo("test@example.com");
        assertThat(member.getLocation()).isEqualTo("서울 강남구");
        assertThat(member.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("추가 정보 등록 — 존재하지 않는 회원: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void registerAdditionalInfo_notFound_throwsException() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        MemberInfoReq req = new MemberInfoReq("test@example.com", "서울", "010-0000-0000");

        // when & then
        assertThatThrownBy(() -> memberWriter.registerAdditionalInfo(MEMBER_ID, req))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("추가 정보 등록 — 이미 탈퇴한 회원: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void registerAdditionalInfo_deletedMember_throwsException() {
        // given
        Member deletedMember = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        deletedMember.softDelete();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(deletedMember));
        MemberInfoReq req = new MemberInfoReq("test@example.com", "서울", "010-0000-0000");

        // when & then
        assertThatThrownBy(() -> memberWriter.registerAdditionalInfo(MEMBER_ID, req))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }

    // ==========================================
    // updateNickname
    // ==========================================

    @Test
    @DisplayName("닉네임 수정: 닉네임이 변경된다")
    void updateNickname_changesNickname() {
        // given
        Member member = Member.createGuest("기존닉네임", "kakao-id", "KAKAO");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        // when
        memberWriter.updateNickname(MEMBER_ID, "새닉네임");

        // then
        assertThat(member.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("닉네임 수정 — 존재하지 않는 회원: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void updateNickname_notFound_throwsException() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWriter.updateNickname(MEMBER_ID, "닉네임"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }

    // ==========================================
    // updateLocation
    // ==========================================

    @Test
    @DisplayName("위치 수정: 위치가 변경된다")
    void updateLocation_changesLocation() {
        // given
        Member member = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        // when
        memberWriter.updateLocation(MEMBER_ID, "경기 성남시");

        // then
        assertThat(member.getLocation()).isEqualTo("경기 성남시");
    }

    @Test
    @DisplayName("위치 수정 — 존재하지 않는 회원: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void updateLocation_notFound_throwsException() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWriter.updateLocation(MEMBER_ID, "서울"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }

    // ==========================================
    // updatePhoneNumber
    // ==========================================

    @Test
    @DisplayName("전화번호 수정: 전화번호가 변경된다")
    void updatePhoneNumber_changesPhoneNumber() {
        // given
        Member member = Member.createGuest("닉네임", "kakao-id", "KAKAO");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        // when
        memberWriter.updatePhoneNumber(MEMBER_ID, "010-9999-8888");

        // then
        assertThat(member.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("전화번호 수정 — 존재하지 않는 회원: MemberException(MEMBER_NOT_FOUND)을 던진다")
    void updatePhoneNumber_notFound_throwsException() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWriter.updatePhoneNumber(MEMBER_ID, "010-0000-0000"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.MEMBER_NOT_FOUND);
    }
}
