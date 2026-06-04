package backend.daangnbasedbackend.member.application.dto;

import backend.daangnbasedbackend.member.domain.Member;
import backend.daangnbasedbackend.member.domain.MemberRole;

public record MemberRes(
        Long memberId,
        String nickname,
        String email,
        String location,
        Double mannerTemp,
        String profileImage,
        String phoneNumber,
        MemberRole role
) {
    public static MemberRes from(Member member) {
        return new MemberRes(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getLocation(),
                member.getMannerTemp(),
                member.getProfileImage(),
                member.getPhoneNumber(),
                member.getRole()
        );
    }
}