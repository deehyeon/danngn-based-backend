package backend.daangnbasedbackend.member.application.provided;

import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;

public interface MemberWriter {
    void registerAdditionalInfo(Long memberId, MemberInfoReq memberInfoReq);
    void updateLocation(Long memberId, String location);
    void updatePhoneNumber(Long memberId, String phoneNumber);
    void updateNickname(Long memberId, String nickname);
}