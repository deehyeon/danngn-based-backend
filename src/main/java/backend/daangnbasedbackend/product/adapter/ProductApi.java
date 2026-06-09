package backend.daangnbasedbackend.product.adapter;

import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.product.application.dto.*;
import backend.daangnbasedbackend.product.domain.ProductState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="PRODUCT", description = "상품 API")
@RequestMapping("/v1/products")
public interface ProductApi {

    @Operation(summary = "특정 상품 조회")
    @GetMapping("/{productId}")
    ApiResponse<ProductRes> getProduct(@PathVariable("productId") Long productId);

    @Operation(summary = "상품 등록(게시)", description = "거래할 상품을 게시한다.")
    @PostMapping
    ApiResponse<?> registerProduct(@AuthenticationPrincipal AuthDetails authDetails, @RequestBody ProductCreateReq req);

    @Operation(summary = "상품 목록 조회", description = "상품들의 목록을 시간순으로 조회한다.")
    @GetMapping
    ApiResponse<Page<ProductSummaryRes>> getProducts(
            @ParameterObject @PageableDefault(size = 10, sort = "refreshAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "상품 정보 수정", description = "등록된 상품의 정보를 수정한다.")
    @PatchMapping("/{productId}")
    ApiResponse<?> updateProduct(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId, @RequestBody ProductUpdateReq req);

    @Operation(summary = "상품 삭제", description = "등록한 product를 삭제한다.")
    @DeleteMapping("/{productId}")
    ApiResponse<?> deleteProduct(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId);

    @Operation(summary = "상품 끌어올리기", description = "등록된 상품을 끌어올리기 한다.")
    @PostMapping("/{productId}/refresh")
    ApiResponse<?> refreshProduct(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId);

    @Operation(summary = "상품 예약하기", description = "상품을 예약중 상태로 변경한다.")
    @PostMapping("/{productId}/reserve")
    ApiResponse<?> reserveProduct(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId, @RequestParam("buyerId") Long buyerId);

    @Operation(summary = "상품 거래 완료", description = "상품을 판매 완료 상태로 변경한다.")
    @PostMapping("/{productId}/complete")
    ApiResponse<?> completeTrade(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId, @RequestParam("buyerId") Long buyerId);

    @Operation(summary = "상품 예약 취소", description = "예약 중인 상품을 다시 판매 중으로 변경한다.")
    @PostMapping("/{productId}/cancel-reservation")
    ApiResponse<?> cancelReservation(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId);

    @Operation(summary = "상품 관심 토글", description = "찜 상태를 토글하고 토글 후 찜 여부와 좋아요 수를 반환한다.")
    @PostMapping("/{productId}/favorite")
    ApiResponse<ToggleFavoriteRes> toggleFavorite(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId);

    @Operation(summary = "상품 찜 여부 조회", description = "현재 사용자가 해당 상품을 찜했는지 여부를 반환한다.")
    @GetMapping("/{productId}/favorite")
    ApiResponse<Boolean> isFavorited(@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("productId") Long productId);

    @Operation(summary = "나의 관심 목록 조회", description = "내가 찜한 상품 목록을 조회한다.")
    @GetMapping("/me/favorites")
    ApiResponse<Page<ProductSummaryRes>> getMyFavoriteProducts(
            @AuthenticationPrincipal AuthDetails authDetails,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "나의 판매 내역 조회", description = "내가 게시한 상품 목록을 조회한다.")
    @GetMapping("/me/selling")
    ApiResponse<Page<ProductSummaryRes>> getMySellingProducts(
            @AuthenticationPrincipal AuthDetails authDetails,
            @RequestParam(value = "state", required = false) ProductState state,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "나의 구매 내역 조회", description = "내가 구매한 상품 목록을 조회한다.")
    @GetMapping("/me/buying")
    ApiResponse<Page<ProductSummaryRes>> getMyBuyingProducts(
            @AuthenticationPrincipal AuthDetails authDetails,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "카테고리 목록 조회", description = "상품 카테고리 전체 목록을 조회한다.")
    @GetMapping("/categories")
    ApiResponse<List<ProductCategoryRes>> getCategories();
}