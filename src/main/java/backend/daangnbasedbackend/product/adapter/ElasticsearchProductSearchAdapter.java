package backend.daangnbasedbackend.product.adapter;

import backend.daangnbasedbackend.product.application.dto.ProductCursor;
import backend.daangnbasedbackend.product.application.required.ProductSearchPort;
import backend.daangnbasedbackend.product.domain.ProductDocument;
import backend.daangnbasedbackend.product.domain.ProductState;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
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
        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .must(QueryBuilders.multiMatch(m -> m.query(keyword).fields("title", "description")))
                .filter(buildFilters(location, state, cursor))
            ))
            .withSort(Sort.by(Sort.Order.desc("refreshedAt"), Sort.Order.desc("productId")))
            .withPageable(PageRequest.ofSize(fetchSize))
            .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
            .getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::getProductId)
            .toList();
    }

    private List<Query> buildFilters(String location, ProductState state, String cursor) {
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
        long cursorMillis = cursor.refreshedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
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
}
