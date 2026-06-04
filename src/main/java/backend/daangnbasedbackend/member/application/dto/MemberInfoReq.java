package backend.daangnbasedbackend.member.application.dto;

public record MemberInfoReq(
        String email,
        String location,
        String phoneNumber
) {}
