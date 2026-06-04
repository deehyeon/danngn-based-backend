package backend.daangnbasedbackend.member.domain;

import backend.daangnbasedbackend.global.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oauth", columnNames = {"oauth_id", "provider"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends AbstractEntity {
    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(nullable = false)
    private String provider; // KAKAO, GOOGLE, APPLE 등

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
        return new Member(
                nickname,
                oauthId,
                provider,
                null,
                null,
                36.5,
                null,
                null,
                MemberRole.GUEST
        );
    }

    public void registerAdditionalInfo(String location, String email, String phoneNumber) {
        this.location = location;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = MemberRole.USER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
