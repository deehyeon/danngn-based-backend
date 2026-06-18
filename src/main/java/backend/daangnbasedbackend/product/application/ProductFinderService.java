package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import backend.daangnbasedbackend.product.application.dto.ProductCursor;
import backend.daangnbasedbackend.product.application.dto.ProductFeedRes;
import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.provided.ProductFinder;
import backend.daangnbasedbackend.product.application.required.ProductCache;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductSearchPort;
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

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFinderService implements ProductFinder {
    private final ProductRepository productRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final MemberFinder memberFinder;
    private final ProductSearchPort productSearchPort;
    private final ProductCache productCache;

    @Override
    public ProductRes findById(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .map(ProductRes::from)
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_NOT_FOUND));
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
    public ProductFeedRes findProductFeed(Long memberId, String cursor, int size, ProductState state) {
        String location = resolveMemberLocation(memberId);
        ProductCursor parsedCursor = cursor != null ? ProductCursor.decode(cursor) : null;

        List<Product> cachedContent = fetchFromCache(location, parsedCursor, size, state);
        boolean cacheHasNext = cachedContent.size() > size;
        if (cacheHasNext) {
            return buildFeedResponse(cachedContent.subList(0, size), true);
        }

        int remainSize = size - cachedContent.size();
        ProductCursor dbCursor = cachedContent.isEmpty()
                ? parsedCursor
                : new ProductCursor(cachedContent.getLast().getRefreshAt(), cachedContent.getLast().getId());
        List<Product> dbResult = fetchFeedFromDb(location, dbCursor, remainSize + 1, state);
        boolean hasNext = dbResult.size() > remainSize;
        List<Product> dbContent = hasNext ? dbResult.subList(0, remainSize) : dbResult;

        List<Product> combined = new ArrayList<>(cachedContent);
        combined.addAll(dbContent);
        return buildFeedResponse(combined, hasNext);
    }

    private List<Product> fetchFromCache(String location, ProductCursor parsedCursor, int size, ProductState state) {
        double maxScore = parsedCursor == null
                ? Double.MAX_VALUE
                : (double) parsedCursor.refreshedAt().toInstant(ZoneOffset.UTC).toEpochMilli();

        List<Long> cachedIds = productCache.getTopProductIds(location, maxScore, size + 1);
        if (cachedIds.isEmpty()) return List.of();

        Map<Long, Product> productMap = productRepository.findAllByIdInAndIsDeletedFalse(cachedIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        return cachedIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .filter(p -> state == null || p.getState() == state)
                .filter(p -> parsedCursor == null || isAfterCursor(p, parsedCursor))
                .limit(size + 1L)
                .toList();
    }

    private boolean isAfterCursor(Product p, ProductCursor cursor) {
        int cmp = p.getRefreshAt().compareTo(cursor.refreshedAt());
        return cmp < 0 || (cmp == 0 && p.getId() < cursor.id());
    }

    private List<Product> fetchFeedFromDb(String location, ProductCursor cursor, int fetchSize, ProductState state) {
        if (cursor == null) {
            return state == null
                    ? productRepository.findFeed(location, PageRequest.ofSize(fetchSize))
                    : productRepository.findFeedByState(location, state, PageRequest.ofSize(fetchSize));
        }
        return state == null
                ? productRepository.findFeedAfterCursor(location, cursor.refreshedAt(), cursor.id(), PageRequest.ofSize(fetchSize))
                : productRepository.findFeedByStateAfterCursor(location, state, cursor.refreshedAt(), cursor.id(), PageRequest.ofSize(fetchSize));
    }

    @Override
    public ProductFeedRes search(Long memberId, String keyword, String cursor, int size, ProductState state) {
        String location = resolveMemberLocation(memberId);
        List<Long> productIds = productSearchPort.searchProductIds(keyword, location, state, cursor, size + 1);
        if (productIds.isEmpty()) return new ProductFeedRes(List.of(), null, false);
        List<Product> products = fetchProductsInEsOrder(productIds);
        boolean hasNext = products.size() > size;
        List<Product> content = hasNext ? products.subList(0, size) : products;
        return buildFeedResponse(content, hasNext);
    }

    private ProductFeedRes buildFeedResponse(List<Product> content, boolean hasNext) {
        String nextCursor = hasNext && !content.isEmpty()
                ? new ProductCursor(content.getLast().getRefreshAt(), content.getLast().getId()).encode()
                : null;
        return new ProductFeedRes(content.stream().map(ProductSummaryRes::from).toList(), nextCursor, hasNext);
    }

    private List<Product> fetchProductsInEsOrder(List<Long> productIds) {
        Map<Long, Product> productMap = productRepository.findAllByIdInAndIsDeletedFalse(productIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
        return productIds.stream()
            .map(productMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private String resolveMemberLocation(Long memberId) {
        String location = memberFinder.findById(memberId).location();
        if (location == null || location.isBlank()) {
            throw new MemberException(MemberErrorType.LOCATION_NOT_SET);
        }
        return location;
    }
}
