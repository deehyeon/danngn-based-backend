package backend.daangnbasedbackend.member.domain;

import backend.daangnbasedbackend.global.domain.AbstractEntity;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Entity
@Getter
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oauth", columnNames = {"oauth_id", "provider"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends AbstractEntity {
    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(nullable = false)
    private String provider;

    @Column(unique = true)
    private String email;

    @Column(length = 100)
    private String location;

    @Column(name = "manner_temp", nullable = false)
    private Double mannerTemp = 36.5;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    private Member(String nickname, String oauthId, String provider, String email, String location, Double mannerTemp, String profileImage, String phoneNumber, MemberRole role) {
        this.nickname = nickname;
        this.oauthId = oauthId;
        this.provider = provider;
        this.email = email;
        this.location = location;
        this.mannerTemp = mannerTemp;
        this.profileImage = profileImage;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public static Member createGuest(String nickname, String oauthId, String provider) {
        validateNickname(nickname);
        return new Member(nickname, oauthId, provider, null, null, 36.5, null, null, MemberRole.GUEST);
    }

    public void registerAdditionalInfo(String location, String email, String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        this.location = location;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = MemberRole.USER;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    public void updatePhoneNumber(String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        this.phoneNumber = phoneNumber;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void applyMannerDelta(Double delta) {
        this.mannerTemp = Math.max(0.0, Math.min(99.0, this.mannerTemp + delta));
    }

    public boolean isGuest() {
        return this.role == MemberRole.GUEST;
    }

    public boolean isUser() {
        return this.role == MemberRole.USER;
    }

    public boolean hasCompletedRegistration() {
        return this.role != MemberRole.GUEST;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.getId().equals(memberId);
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new MemberException(MemberErrorType.INVALID_NAME);
        }
        if (nickname.length() > 20) {
            throw new MemberException(MemberErrorType.NICKNAME_TOO_LONG);
        }
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$").matcher(phoneNumber).matches()) {
            throw new MemberException(MemberErrorType.INVALID_PHONE_NUMBER);
        }
    }
}
