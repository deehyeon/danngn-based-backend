package backend.daangnbasedbackend.member.adapter;

import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.member.application.dto.MemberInfoReq;
import backend.daangnbasedbackend.member.application.dto.MemberRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "MEMBER", description = "회원 정보 API")
@RequestMapping("/v1/members")
public interface MemberApi {

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 회원의 프로필을 조회합니다.")
    @GetMapping("/me")
    ApiResponse<MemberRes> getMyProfile(@AuthenticationPrincipal AuthDetails authDetails);

    @Operation(summary = "추가 정보 등록", description = "소셜 로그인 후 위치, 이메일, 전화번호를 등록하고 GUEST에서 USER로 승격됩니다.")
    @PostMapping("/me/additional-info")
    ApiResponse<?> registerAdditionalInfo(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid MemberInfoReq req);

    @Operation(summary = "닉네임 수정", description = "회원의 닉네임을 수정합니다.")
    @PatchMapping("/me/nickname")
    ApiResponse<?> updateNickname(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid String nickname);

    @Operation(summary = "위치 수정", description = "회원의 위치를 수정합니다.")
    @PatchMapping("/me/location")
    ApiResponse<?> updateLocation(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid String location);

    @Operation(summary = "이메일 수정", description = "회원의 이메일을 수정합니다.")
    @PatchMapping("/me/email")
    ApiResponse<?> updateEmail(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid String email);

    @Operation(summary = "전화번호 수정", description = "회원의 전화번호를 수정합니다.")
    @PatchMapping("/me/phone-number")
    ApiResponse<?> updatePhoneNumber(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid String phoneNumber);

    @Operation(summary = "프로필 이미지 수정", description = "회원의 프로필 이미지 URL을 수정합니다.")
    @PatchMapping("/me/profile-image")
    ApiResponse<?> updateProfileImage(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody @Valid String profileImageUrl);
}
