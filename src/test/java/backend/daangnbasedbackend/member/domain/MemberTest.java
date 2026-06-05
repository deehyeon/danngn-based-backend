package backend.daangnbasedbackend.member.domain;

import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    // ==========================================
    // createGuest
    // ==========================================

    @Test
    @DisplayName("createGuest: GUEST 역할과 기본값으로 회원을 생성한다")
    void createGuest_setsGuestRoleAndNullFields() {
        Member member = Member.createGuest("닉네임", "kakao-id-1", "KAKAO");

        assertThat(member.getNickname()).isEqualTo("닉네임");
        assertThat(member.getOauthId()).isEqualTo("kakao-id-1");
        assertThat(member.getProvider()).isEqualTo("KAKAO");
        assertThat(member.getRole()).isEqualTo(MemberRole.GUEST);
        assertThat(member.getEmail()).isNull();
        assertThat(member.getLocation()).isNull();
        assertThat(member.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("createGuest: 매너온도 기본값은 36.5이다")
    void createGuest_defaultMannerTempIs36point5() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThat(member.getMannerTemp()).isEqualTo(36.5);
    }

    @Test
    @DisplayName("createGuest: 닉네임이 null이면 INVALID_NAME 예외를 던진다")
    void createGuest_nullNickname_throwsInvalidName() {
        assertThatThrownBy(() -> Member.createGuest(null, "id", "KAKAO"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.INVALID_NAME);
    }

    @Test
    @DisplayName("createGuest: 닉네임이 공백이면 INVALID_NAME 예외를 던진다")
    void createGuest_blankNickname_throwsInvalidName() {
        assertThatThrownBy(() -> Member.createGuest("   ", "id", "KAKAO"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.INVALID_NAME);
    }

    @Test
    @DisplayName("createGuest: 닉네임이 20자를 초과하면 NICKNAME_TOO_LONG 예외를 던진다")
    void createGuest_nicknameTooLong_throwsNicknameTooLong() {
        String longNickname = "가".repeat(21);

        assertThatThrownBy(() -> Member.createGuest(longNickname, "id", "KAKAO"))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.NICKNAME_TOO_LONG);
    }

    @Test
    @DisplayName("createGuest: 닉네임이 정확히 20자이면 생성에 성공한다")
    void createGuest_nicknameExactly20Chars_succeeds() {
        String nickname = "가".repeat(20);

        Member member = Member.createGuest(nickname, "id", "KAKAO");

        assertThat(member.getNickname()).isEqualTo(nickname);
    }

    // ==========================================
    // registerAdditionalInfo
    // ==========================================

    @Test
    @DisplayName("registerAdditionalInfo: 추가 정보가 저장되고 역할이 USER로 승격된다")
    void registerAdditionalInfo_savesInfoAndUpgradesRole() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.registerAdditionalInfo("서울 마포구", "hello@example.com", "010-1234-5678");

        assertThat(member.getLocation()).isEqualTo("서울 마포구");
        assertThat(member.getEmail()).isEqualTo("hello@example.com");
        assertThat(member.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("registerAdditionalInfo: 전화번호가 null이면 검증을 통과한다")
    void registerAdditionalInfo_nullPhone_succeeds() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.registerAdditionalInfo("서울", "test@example.com", null);

        assertThat(member.getPhoneNumber()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "010-12345-678", "02-1234-5678", "010-1234-56789", "abcd"})
    @DisplayName("registerAdditionalInfo: 올바르지 않은 전화번호 형식이면 INVALID_PHONE_NUMBER 예외를 던진다")
    void registerAdditionalInfo_invalidPhone_throwsInvalidPhoneNumber(String invalidPhone) {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThatThrownBy(() -> member.registerAdditionalInfo("서울", "test@example.com", invalidPhone))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.INVALID_PHONE_NUMBER);
    }

    // ==========================================
    // updateNickname
    // ==========================================

    @Test
    @DisplayName("updateNickname: 닉네임이 변경된다")
    void updateNickname_changesNickname() {
        Member member = Member.createGuest("기존닉네임", "id", "KAKAO");

        member.updateNickname("새닉네임");

        assertThat(member.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("updateNickname: 닉네임이 null이면 INVALID_NAME 예외를 던진다")
    void updateNickname_null_throwsInvalidName() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThatThrownBy(() -> member.updateNickname(null))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.INVALID_NAME);
    }

    @Test
    @DisplayName("updateNickname: 닉네임이 20자를 초과하면 NICKNAME_TOO_LONG 예외를 던진다")
    void updateNickname_tooLong_throwsNicknameTooLong() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThatThrownBy(() -> member.updateNickname("가".repeat(21)))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.NICKNAME_TOO_LONG);
    }

    @Test
    @DisplayName("updateNickname: 닉네임이 정확히 20자이면 변경에 성공한다")
    void updateNickname_exactly20Chars_succeeds() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        String nickname = "가".repeat(20);

        member.updateNickname(nickname);

        assertThat(member.getNickname()).isEqualTo(nickname);
    }

    // ==========================================
    // updatePhoneNumber
    // ==========================================

    @Test
    @DisplayName("updatePhoneNumber: 전화번호가 변경된다")
    void updatePhoneNumber_changesPhoneNumber() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updatePhoneNumber("010-9999-8888");

        assertThat(member.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "011-123-4567", "016-1234-5678", "019-1234-5678"})
    @DisplayName("updatePhoneNumber: 유효한 전화번호 형식은 변경에 성공한다")
    void updatePhoneNumber_validFormats_succeeds(String validPhone) {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updatePhoneNumber(validPhone);

        assertThat(member.getPhoneNumber()).isEqualTo(validPhone);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "010-12345-678", "02-1234-5678", "abcd"})
    @DisplayName("updatePhoneNumber: 올바르지 않은 전화번호 형식이면 INVALID_PHONE_NUMBER 예외를 던진다")
    void updatePhoneNumber_invalidFormat_throwsInvalidPhoneNumber(String invalidPhone) {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThatThrownBy(() -> member.updatePhoneNumber(invalidPhone))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.INVALID_PHONE_NUMBER);
    }

    @Test
    @DisplayName("updatePhoneNumber: null이면 검증을 통과한다")
    void updatePhoneNumber_null_succeeds() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.updatePhoneNumber(null);

        assertThat(member.getPhoneNumber()).isNull();
    }

    // ==========================================
    // applyMannerDelta
    // ==========================================

    @Test
    @DisplayName("applyMannerDelta: 양수 delta가 매너온도에 누적된다")
    void applyMannerDelta_positiveDelta_increases() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.applyMannerDelta(0.1);

        assertThat(member.getMannerTemp()).isEqualTo(36.6, within(0.001));
    }

    @Test
    @DisplayName("applyMannerDelta: 음수 delta가 매너온도에 누적된다")
    void applyMannerDelta_negativeDelta_decreases() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.applyMannerDelta(-1.0);

        assertThat(member.getMannerTemp()).isEqualTo(35.5, within(0.001));
    }

    @Test
    @DisplayName("applyMannerDelta: 매너온도는 0.0 아래로 내려가지 않는다")
    void applyMannerDelta_doesNotGoBelowMin() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.applyMannerDelta(-100.0);

        assertThat(member.getMannerTemp()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("applyMannerDelta: 매너온도는 99.0을 초과하지 않는다")
    void applyMannerDelta_doesNotExceedMax() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.applyMannerDelta(100.0);

        assertThat(member.getMannerTemp()).isEqualTo(99.0);
    }

    // ==========================================
    // 역할 확인 메서드
    // ==========================================

    @Test
    @DisplayName("isGuest: GUEST 역할이면 true를 반환한다")
    void isGuest_returnsTrue_whenGuest() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThat(member.isGuest()).isTrue();
    }

    @Test
    @DisplayName("isGuest: USER 역할이면 false를 반환한다")
    void isGuest_returnsFalse_whenUser() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        member.registerAdditionalInfo("서울", "test@example.com", null);

        assertThat(member.isGuest()).isFalse();
    }

    @Test
    @DisplayName("isUser: USER 역할이면 true를 반환한다")
    void isUser_returnsTrue_whenUser() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        member.registerAdditionalInfo("서울", "test@example.com", null);

        assertThat(member.isUser()).isTrue();
    }

    @Test
    @DisplayName("hasCompletedRegistration: USER 역할이면 true를 반환한다")
    void hasCompletedRegistration_returnsTrue_whenUser() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        member.registerAdditionalInfo("서울", "test@example.com", null);

        assertThat(member.hasCompletedRegistration()).isTrue();
    }

    @Test
    @DisplayName("hasCompletedRegistration: GUEST 역할이면 false를 반환한다")
    void hasCompletedRegistration_returnsFalse_whenGuest() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        assertThat(member.hasCompletedRegistration()).isFalse();
    }

    // ==========================================
    // isOwnedBy
    // ==========================================

    @Test
    @DisplayName("isOwnedBy: 동일한 memberId이면 true를 반환한다")
    void isOwnedBy_sameId_returnsTrue() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        ReflectionTestUtils.setField(member, "id", 1L);

        assertThat(member.isOwnedBy(1L)).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy: 다른 memberId이면 false를 반환한다")
    void isOwnedBy_differentId_returnsFalse() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");
        ReflectionTestUtils.setField(member, "id", 1L);

        assertThat(member.isOwnedBy(2L)).isFalse();
    }

    // ==========================================
    // softDelete
    // ==========================================

    @Test
    @DisplayName("softDelete: isDeleted가 true가 된다")
    void softDelete_setsIsDeletedTrue() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.softDelete();

        assertThat(member.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("softDelete: 여러 번 호출해도 isDeleted는 true를 유지한다")
    void softDelete_idempotent() {
        Member member = Member.createGuest("닉네임", "id", "KAKAO");

        member.softDelete();
        member.softDelete();

        assertThat(member.getIsDeleted()).isTrue();
    }

    private static org.assertj.core.data.Offset<Double> within(double precision) {
        return org.assertj.core.data.Offset.offset(precision);
    }
}
