package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.provided.ProductFinder;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.ProductState;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFinderService implements ProductFinder {
    private final ProductRepository productRepository;
    private final FavoriteProductRepository favoriteProductRepository;

    @Override
    public ProductRes findById(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.getIsDeleted())
                .map(ProductRes::from)
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_NOT_FOUND));
    }

    @Override
    public Page<ProductSummaryRes> findProducts(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable)
                .map(ProductSummaryRes::from);
    }

    @Override
    public Page<ProductSummaryRes> findProductsBySellerId(Long sellerId, ProductState state, Pageable pageable) {
        return productRepository.findBySellerIdAndStateAndIsDeletedFalse(sellerId, state, pageable)
                .map(ProductSummaryRes::from);
    }

    @Override
    public Page<ProductSummaryRes> findProductsByBuyerId(Long buyerId, Pageable pageable) {
        return productRepository.findByBuyerIdAndIsDeletedFalse(buyerId, pageable)
                .map(ProductSummaryRes::from);
    }

    @Override
    public Page<ProductSummaryRes> findFavoriteProducts(Long memberId, Pageable pageable) {
        return productRepository.findFavoritesByMemberId(memberId, pageable)
                .map(ProductSummaryRes::from);
    }

    @Override
    public boolean isFavorited(Long memberId, Long productId) {
        return favoriteProductRepository.existsByMemberIdAndProductId(memberId, productId);
    }
}
