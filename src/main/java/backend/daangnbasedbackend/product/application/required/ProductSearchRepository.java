package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
}