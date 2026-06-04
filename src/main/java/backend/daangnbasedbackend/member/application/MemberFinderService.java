package backend.daangnbasedbackend.member.application;

import backend.daangnbasedbackend.member.application.dto.MemberRes;
import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.application.required.MemberRepository;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberFinderService implements MemberFinder {
    private final MemberRepository memberRepository;

    @Override
    public MemberRes findById(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberRes::from)
                .orElseThrow(() -> new MemberException(MemberErrorType.MEMBER_NOT_FOUND));
    }

    @Override
    public MemberRes findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(MemberRes::from)
                .orElseThrow(() -> new MemberException(MemberErrorType.MEMBER_NOT_FOUND));
    }
}
