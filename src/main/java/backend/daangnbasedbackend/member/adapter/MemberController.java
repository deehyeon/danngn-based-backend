package backend.daangnbasedbackend.member.adapter;

import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;
import backend.daangnbasedbackend.member.application.dto.MemberRes;
import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.application.provided.MemberWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {
    private final MemberWriter memberWriter;
    private final MemberFinder memberFinder;

    @Override
    public ApiResponse<MemberRes> getMyProfile(@AuthenticationPrincipal AuthDetails authDetails) {
        MemberRes result = memberFinder.findById(authDetails.getMemberId());
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<?> registerAdditionalInfo(@AuthenticationPrincipal AuthDetails authDetails, MemberInfoReq req) {
        memberWriter.registerAdditionalInfo(authDetails.getMemberId(), req);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updateNickname(@AuthenticationPrincipal AuthDetails authDetails, String nickname) {
        memberWriter.updateNickname(authDetails.getMemberId(), nickname);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updateLocation(@AuthenticationPrincipal AuthDetails authDetails, String location) {
        memberWriter.updateLocation(authDetails.getMemberId(), location);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updateEmail(@AuthenticationPrincipal AuthDetails authDetails, String email) {
        memberWriter.updateEmail(authDetails.getMemberId(), email);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updatePhoneNumber(@AuthenticationPrincipal AuthDetails authDetails, String phoneNumber) {
        memberWriter.updatePhoneNumber(authDetails.getMemberId(), phoneNumber);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updateProfileImage(@AuthenticationPrincipal AuthDetails authDetails, String profileImageUrl) {
        memberWriter.updateProfileImage(authDetails.getMemberId(), profileImageUrl);
        return ApiResponse.success();
    }
}
