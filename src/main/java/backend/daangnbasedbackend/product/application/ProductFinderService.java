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
import backend.daangnbasedbackend.product.domain.ProductDocument;
import backend.daangnbasedbackend.product.domain.ProductState;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
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
    private final ElasticsearchOperations elasticsearchOperations;

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
        List<Product> products = fetchFeedProducts(location, cursor, size + 1, state);
        return buildFeedResponse(products, size);
    }

    @Override
    public ProductFeedRes search(Long memberId, String keyword, String cursor, int size, ProductState state) {
        String location = resolveMemberLocation(memberId);
        List<Query> filters = buildSearchFilters(location, state, cursor);
        NativeQuery query = buildSearchQuery(keyword, filters, size + 1);

        List<Long> productIds = elasticsearchOperations.search(query, ProductDocument.class)
            .getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::getProductId)
            .toList();

        if (productIds.isEmpty()) return new ProductFeedRes(List.of(), null, false);

        return buildFeedResponse(fetchProductsInEsOrder(productIds), size);
    }

    private List<Product> fetchFeedProducts(String location, String cursor, int fetchSize, ProductState state) {
        if (cursor == null) {
            return state == null
                ? productRepository.findFeed(location, PageRequest.ofSize(fetchSize))
                : productRepository.findFeedByState(location, state, PageRequest.ofSize(fetchSize));
        }
        ProductCursor productCursor = ProductCursor.decode(cursor);
        return state == null
            ? productRepository.findFeedAfterCursor(location, productCursor.refreshedAt(), productCursor.id(), PageRequest.ofSize(fetchSize))
            : productRepository.findFeedByStateAfterCursor(location, state, productCursor.refreshedAt(), productCursor.id(), PageRequest.ofSize(fetchSize));
    }

    private ProductFeedRes buildFeedResponse(List<Product> products, int size) {
        boolean hasNext = products.size() > size;
        List<Product> content = hasNext ? products.subList(0, size) : products;
        String nextCursor = hasNext
            ? new ProductCursor(content.get(content.size() - 1).getRefreshAt(), content.get(content.size() - 1).getId()).encode()
            : null;
        return new ProductFeedRes(content.stream().map(ProductSummaryRes::from).toList(), nextCursor, hasNext);
    }

    private List<Query> buildSearchFilters(String location, ProductState state, String cursor) {
        List<Query> filters = new ArrayList<>();
        filters.add(QueryBuilders.term(t -> t.field("location").value(location)));
        if (state != null) {
            filters.add(QueryBuilders.term(t -> t.field("state").value(state.name())));
        }
        if (cursor != null) {
            filters.add(buildCursorFilter(ProductCursor.decode(cursor)));
        }
        return filters;
    }

    private Query buildCursorFilter(ProductCursor cursor) {
        long cursorMillis = cursor.refreshedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long cursorId = cursor.id();
        return QueryBuilders.bool(b -> b
            .should(QueryBuilders.range(r -> r.untyped(u -> u.field("refreshedAt").lt(JsonData.of(cursorMillis)))))
            .should(QueryBuilders.bool(inner -> inner
                .must(QueryBuilders.range(ir -> ir.untyped(u -> u.field("refreshedAt").gte(JsonData.of(cursorMillis)).lte(JsonData.of(cursorMillis)))))
                .must(QueryBuilders.range(ir -> ir.untyped(u -> u.field("productId").lt(JsonData.of(cursorId)))))
            ))
            .minimumShouldMatch("1")
        );
    }

    private NativeQuery buildSearchQuery(String keyword, List<Query> filters, int fetchSize) {
        return NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .must(QueryBuilders.multiMatch(m -> m.query(keyword).fields("title", "description")))
                .filter(filters)
            ))
            .withSort(Sort.by(Sort.Order.desc("refreshedAt"), Sort.Order.desc("productId")))
            .withPageable(PageRequest.ofSize(fetchSize))
            .build();
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
