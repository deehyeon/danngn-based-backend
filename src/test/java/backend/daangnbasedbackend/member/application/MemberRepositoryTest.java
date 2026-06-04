package backend.daangnbasedbackend.member.application;

import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("oauthId와 provider로 회원을 조회한다")
    void findByOauthIdAndProvider_returnsMatchingMember() {
        // given
        Member member = Member.createGuest("테스트유저", "kakao-social-id", "KAKAO");
        memberRepository.save(member);

        // when
        Optional<Member> found = memberRepository.findByOauthIdAndProvider("kakao-social-id", "KAKAO");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProvider()).isEqualTo("KAKAO");
    }

    @Test
    @DisplayName("존재하지 않는 oauthId: Optional.empty를 반환한다")
    void findByOauthIdAndProvider_notFound_returnsEmpty() {
        // when
        Optional<Member> found = memberRepository.findByOauthIdAndProvider("unknown-id", "KAKAO");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("다른 provider의 동일 oauthId: 조회되지 않는다")
    void findByOauthIdAndProvider_differentProvider_returnsEmpty() {
        // given
        Member member = Member.createGuest("테스트유저", "social-id-1", "KAKAO");
        memberRepository.save(member);

        // when
        Optional<Member> found = memberRepository.findByOauthIdAndProvider("social-id-1", "GOOGLE");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("회원 저장 시 createdAt, isDeleted 기본값이 설정된다")
    void save_setsAuditingFieldsAndDefaultIsDeleted() {
        // given
        Member member = Member.createGuest("테스트유저", "social-id-audit", "KAKAO");

        // when
        Member saved = memberRepository.save(member);

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("softDelete 후 isDeleted가 true로 변경된다")
    void softDelete_setsIsDeletedTrue() {
        // given
        Member member = Member.createGuest("테스트유저", "social-id-delete", "KAKAO");
        memberRepository.save(member);

        // when
        member.softDelete();
        memberRepository.flush();

        // then
        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("registerAdditionalInfo 호출 후 role이 USER로 변경된다")
    void registerAdditionalInfo_upgradesRoleToUser() {
        // given
        Member member = Member.createGuest("테스트유저", "social-id-register", "KAKAO");
        memberRepository.save(member);

        // when
        member.registerAdditionalInfo("서울 강남구", "test@example.com", "010-0000-0000");
        memberRepository.flush();

        // then
        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getLocation()).isEqualTo("서울 강남구");
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }
}
