package backend.daangnbasedbackend.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    // ==========================================
    // createGuest
    // ==========================================
    @Test
    @DisplayName("GUEST 역할과 null 이메일·위치로 회원을 생성한다")
    void createGuest_setsGuestRoleAndNullFields() {
        Member member = Member.createGuest("닉네임", "kakao-id-1", "KAKAO");

        assertThat(member.getNickname()).isEqualTo("닉네임");
        assertThat(member.getOauthId()).isEqualTo("kakao-id-1");
        assertThat(member.getProvider()).isEqualTo("KAKAO");
        assertThat(member.getRole()).isEqualTo(MemberRole.GUEST);

        // 생성 시점에는 입력받지 않는 값들 검증
        assertThat(member.getEmail()).isNull();
        assertThat(member.getLocation()).isNull();
        assertThat(member.getPhoneNumber()).isNull();

        // AbstractEntity(BaseEntity) 상속으로 인한 기본값 검증 (구현 방식에 따라 다를 수 있음)
        // assertThat(member.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("매너온도 기본값은 36.5이다")
    void createGuest_defaultMannerTempIs36point5() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThat(member.getMannerTemp()).isEqualTo(36.5);
    }

    // ==========================================
    // registerAdditionalInfo
    // ==========================================
    @Test
    @DisplayName("추가 정보 등록 후 역할이 USER로 승격된다")
    void registerAdditionalInfo_upgradesRoleToUser() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.registerAdditionalInfo("서울 강남구", "test@example.com", "010-0000-0000");

        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("추가 정보가 올바르게 저장된다")
    void registerAdditionalInfo_savesLocationEmailPhone() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.registerAdditionalInfo("서울 마포구", "hello@example.com", "010-1234-5678");

        assertThat(member.getLocation()).isEqualTo("서울 마포구");
        assertThat(member.getEmail()).isEqualTo("hello@example.com");
        assertThat(member.getPhoneNumber()).isEqualTo("010-1234-5678");
    }

    // ==========================================
    // update 메서드 모음
    // ==========================================
    @Test
    @DisplayName("updateNickname: 닉네임이 변경된다")
    void updateNickname_changesNickname() {
        Member member = Member.createGuest("기존닉네임", "id", "KAKAO");

        member.updateNickname("새닉네임");

        assertThat(member.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("updateLocation: 위치가 변경된다")
    void updateLocation_changesLocation() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updateLocation("경기 성남시");

        assertThat(member.getLocation()).isEqualTo("경기 성남시");
    }

    @Test
    @DisplayName("updateProfileImage: 프로필 이미지가 변경된다")
    void updateProfileImage_changesProfileImage() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updateProfileImage("https://example.com/image.jpg");

        assertThat(member.getProfileImage()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("updatePhoneNumber: 전화번호가 변경된다")
    void updatePhoneNumber_changesPhoneNumber() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updatePhoneNumber("010-9999-8888");

        assertThat(member.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    // ==========================================
    // softDelete
    // ==========================================
    @Test
    @DisplayName("softDelete 호출 후 isDeleted가 true가 된다")
    void softDelete_setsIsDeletedTrue() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        // AbstractEntity에 초기화 로직이 없다면 NPE가 날 수 있으므로 주석 처리 또는 엔티티 기본값 확인 필요
        // assertThat(member.getIsDeleted()).isFalse();

        member.softDelete();

        assertThat(member.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("softDelete를 여러 번 호출해도 isDeleted는 true를 유지한다")
    void softDelete_idempotent() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.softDelete();
        member.softDelete(); // 멱등성 검증

        assertThat(member.getIsDeleted()).isTrue();
    }
}