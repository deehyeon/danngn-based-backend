package backend.daangnbasedbackend.member.application.provided;

import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;

public interface MemberWriter {
    void registerAdditionalInfo(Long memberId, MemberInfoReq memberInfoReq);
    void updateNickname(Long memberId, String nickname);
    void updateLocation(Long memberId, String location);
    void updateEmail(Long memberId, String email);
    void updatePhoneNumber(Long memberId, String phoneNumber);
    void updateProfileImage(Long memberId, String profileImageUrl);
    void applyMannerDelta(Long memberId, Double delta);
}