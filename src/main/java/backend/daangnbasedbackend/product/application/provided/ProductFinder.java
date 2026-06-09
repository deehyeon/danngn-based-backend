package backend.daangnbasedbackend.product.application.provided;

import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.domain.ProductState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductFinder {
    ProductRes findById(Long productId);
    Page<ProductSummaryRes> findProducts(Pageable pageable);
    Page<ProductSummaryRes> findProductsBySellerId(Long sellerId, ProductState state, Pageable pageable);
    Page<ProductSummaryRes> findProductsByBuyerId(Long buyerId, Pageable pageable);
    Page<ProductSummaryRes> findFavoriteProducts(Long memberId, Pageable pageable);
    boolean isFavorited(Long memberId, Long productId);
}
