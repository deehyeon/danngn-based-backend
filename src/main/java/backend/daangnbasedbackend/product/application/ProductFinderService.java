package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import backend.daangnbasedbackend.product.application.dto.ProductCursor;
import backend.daangnbasedbackend.product.application.dto.ProductFeedRes;
import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.provided.ProductFinder;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductState;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFinderService implements ProductFinder {
    private final ProductRepository productRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final MemberFinder memberFinder;

    @Override
    public ProductRes findById(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
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
        return favoriteProductRepository.existsByMemberIdAndProductIdAndIsDeletedFalse(memberId, productId);
    }

    @Override
    public ProductFeedRes findFeed(Long memberId, String cursor, int size, ProductState state) {
        String location = resolveMemberLocation(memberId);
        int fetchSize = size + 1;

        List<Product> products;
        if (cursor == null) {
            products = state == null
                ? productRepository.findFeed(location, PageRequest.ofSize(fetchSize))
                : productRepository.findFeedByState(location, state, PageRequest.ofSize(fetchSize));
        } else {
            ProductCursor productCursor = ProductCursor.decode(cursor);
            products = state == null
                ? productRepository.findFeedAfterCursor(location, productCursor.refreshedAt(), productCursor.id(), PageRequest.ofSize(fetchSize))
                : productRepository.findFeedByStateAfterCursor(location, state, productCursor.refreshedAt(), productCursor.id(), PageRequest.ofSize(fetchSize));
        }

        boolean hasNext = products.size() > size;
        List<Product> content = hasNext ? products.subList(0, size) : products;

        String nextCursor = hasNext
            ? new ProductCursor(content.get(content.size() - 1).getRefreshAt(), content.get(content.size() - 1).getId()).encode()
            : null;

        return new ProductFeedRes(
            content.stream().map(ProductSummaryRes::from).toList(),
            nextCursor,
            hasNext
        );
    }

    private String resolveMemberLocation(Long memberId) {
        String location = memberFinder.findById(memberId).location();
        if (location == null || location.isBlank()) {
            throw new MemberException(MemberErrorType.LOCATION_NOT_SET);
        }
        return location;
    }
}
