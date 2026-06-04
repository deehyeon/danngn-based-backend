package backend.daangnbasedbackend.member.application.provided;

import backend.daangnbasedbackend.member.application.dto.MemberRes;

public interface MemberFinder {
    MemberRes findById(Long memberId);
    MemberRes findByEmail(String email);
}
