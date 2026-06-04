package backend.daangnbasedbackend.member.application;

import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;
import backend.daangnbasedbackend.member.application.provided.MemberWriter;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.domain.Member;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberWriterService implements MemberWriter {
    private final MemberRepository memberRepository;

    @Override
    public void registerAdditionalInfo(Long memberId, MemberInfoReq memberInfoReq) {
        Member member = findById(memberId);
        member.registerAdditionalInfo(memberInfoReq.location(),  memberInfoReq.email(), memberInfoReq.phoneNumber());
    }

    @Override
    public void updateLocation(Long memberId, String location) {
        Member member = findById(memberId);
        member.updateLocation(location);
    }

    @Override
    public void updatePhoneNumber(Long memberId, String phoneNumber) {
        Member member = findById(memberId);
        member.updatePhoneNumber(phoneNumber);
    }

    @Override
    public void updateNickname(Long memberId, String nickname) {
        Member member = findById(memberId);
        member.updateNickname(nickname);
    }

    private Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new MemberException(MemberErrorType.MEMBER_NOT_FOUND));
    }
}
