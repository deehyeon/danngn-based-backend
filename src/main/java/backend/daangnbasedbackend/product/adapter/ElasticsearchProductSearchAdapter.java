package backend.daangnbasedbackend.product.adapter;

import backend.daangnbasedbackend.product.application.dto.ProductCursor;
import backend.daangnbasedbackend.product.application.required.ProductSearchPort;
import backend.daangnbasedbackend.product.domain.ProductDocument;
import backend.daangnbasedbackend.product.domain.ProductState;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ElasticsearchProductSearchAdapter implements ProductSearchPort {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<Long> searchProductIds(String keyword, String location, ProductState state, String cursor, int fetchSize) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .must(QueryBuilders.multiMatch(m -> m.query(keyword).fields("title", "description")))
                .filter(buildFilters(location, state))
            ))
            .withSort(Sort.by(Sort.Order.desc("refreshedAt"), Sort.Order.desc("productId")))
            .withPageable(PageRequest.ofSize(fetchSize));

        if (cursor != null) {
            ProductCursor decoded = ProductCursor.decode(cursor);
            long epochMillis = decoded.refreshedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            queryBuilder.withSearchAfter(List.of(epochMillis, decoded.id()));
        }

        return elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class)
            .getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::getProductId)
            .toList();
    }

    private List<Query> buildFilters(String location, ProductState state) {
        List<Query> filters = new ArrayList<>();
        filters.add(QueryBuilders.term(t -> t.field("location").value(location)));
        if (state != null) {
            filters.add(QueryBuilders.term(t -> t.field("state").value(state.name())));
        }
        return filters;
    }
}
