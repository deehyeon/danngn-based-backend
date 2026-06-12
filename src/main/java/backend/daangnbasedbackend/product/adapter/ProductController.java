package backend.daangnbasedbackend.product.adapter;

import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import backend.daangnbasedbackend.product.application.dto.ProductCategoryRes;
import backend.daangnbasedbackend.product.application.dto.ProductCreateReq;
import backend.daangnbasedbackend.product.application.dto.ProductFeedRes;
import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.dto.ProductUpdateReq;
import backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes;
import backend.daangnbasedbackend.product.application.provided.FavoriteWriter;
import backend.daangnbasedbackend.product.application.provided.ProductCategoryFinder;
import backend.daangnbasedbackend.product.application.provided.ProductFinder;
import backend.daangnbasedbackend.product.application.provided.ProductWriter;
import backend.daangnbasedbackend.product.domain.ProductState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {
    private final FavoriteWriter favoriteWriter;
    private final ProductWriter productWriter;
    private final ProductFinder productFinder;
    private final ProductCategoryFinder productCategoryFinder;

    @Override
    public ApiResponse<ProductRes> getProduct(Long productId) {
        return ApiResponse.success(productFinder.findById(productId));
    }

    @Override
    public ApiResponse<?> registerProduct(AuthDetails authDetails, ProductCreateReq req) {
        productWriter.create(authDetails.getMemberId(), req);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> updateProduct(AuthDetails authDetails, Long productId, ProductUpdateReq req) {
        productWriter.update(authDetails.getMemberId(), productId, req);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> deleteProduct(AuthDetails authDetails, Long productId) {
        productWriter.delete(authDetails.getMemberId(), productId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> refreshProduct(AuthDetails authDetails, Long productId) {
        productWriter.refresh(authDetails.getMemberId(), productId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> reserveProduct(AuthDetails authDetails, Long productId, Long buyerId) {
        productWriter.reserve(authDetails.getMemberId(), productId, buyerId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> completeTrade(AuthDetails authDetails, Long productId, Long buyerId) {
        productWriter.completeTrade(authDetails.getMemberId(), productId, buyerId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<?> cancelReservation(AuthDetails authDetails, Long productId) {
        productWriter.cancelReservation(authDetails.getMemberId(), productId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<ToggleFavoriteRes> toggleFavorite(AuthDetails authDetails, Long productId) {
        return ApiResponse.success(favoriteWriter.toggleFavorite(authDetails.getMemberId(), productId));
    }

    @Override
    public ApiResponse<Boolean> isFavorited(AuthDetails authDetails, Long productId) {
        return ApiResponse.success(productFinder.isFavorited(authDetails.getMemberId(), productId));
    }

    @Override
    public ApiResponse<Page<ProductSummaryRes>> getMyFavoriteProducts(AuthDetails authDetails, Pageable pageable) {
        return ApiResponse.success(productFinder.findFavoriteProducts(authDetails.getMemberId(), pageable));
    }

    @Override
    public ApiResponse<Page<ProductSummaryRes>> getMySellingProducts(AuthDetails authDetails, ProductState state, Pageable pageable) {
        return ApiResponse.success(productFinder.findProductsBySellerId(authDetails.getMemberId(), state, pageable));
    }

    @Override
    public ApiResponse<Page<ProductSummaryRes>> getMyBuyingProducts(AuthDetails authDetails, Pageable pageable) {
        return ApiResponse.success(productFinder.findProductsByBuyerId(authDetails.getMemberId(), pageable));
    }

    @Override
    public ApiResponse<List<ProductCategoryRes>> getCategories() {
        return ApiResponse.success(productCategoryFinder.findAll());
    }

    @Override
    public ApiResponse<ProductFeedRes> getProductFeed(AuthDetails authDetails, String cursor, int size, ProductState state) {
        return ApiResponse.success(productFinder.findProductFeed(authDetails.getMemberId(), cursor, size, state));
    }

    @Override
    public ApiResponse<ProductFeedRes> searchProducts(AuthDetails authDetails, String keyword, String cursor, int size, ProductState state) {
        return ApiResponse.success(productFinder.search(authDetails.getMemberId(), keyword, cursor, size, state));
    }
}
