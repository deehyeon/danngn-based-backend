package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductSearchRepository;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import backend.daangnbasedbackend.product.domain.ProductDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductIndexEventListenerTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductSearchRepository productSearchRepository;

    private ProductIndexEventListener listener;

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        listener = new ProductIndexEventListener(productRepository, productSearchRepository, retryTemplate);
    }

    private Product activeProduct(Long id) {
        Product p = Product.create(1L, ProductCategory.create("디지털기기"), "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("handle(upsert): 활성 상품을 ES 인덱스에 저장한다")
    void handle_upsert_savesDocument() {
        // given
        Product product = activeProduct(10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // when
        listener.handle(ProductIndexEvent.upsert(10L));

        // then
        verify(productSearchRepository).save(any(ProductDocument.class));
        verify(productSearchRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("handle(upsert): 소프트 삭제된 상품은 ES 인덱스에서 제거한다")
    void handle_upsert_softDeletedProduct_deletesFromIndex() {
        // given
        Product product = activeProduct(10L);
        product.softDelete();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // when
        listener.handle(ProductIndexEvent.upsert(10L));

        // then
        verify(productSearchRepository).deleteById(10L);
        verify(productSearchRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle(upsert): DB에서 상품을 찾을 수 없으면 ES 작업을 수행하지 않는다")
    void handle_upsert_productNotFound_noEsOperation() {
        // given
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // when
        listener.handle(ProductIndexEvent.upsert(10L));

        // then
        verify(productSearchRepository, never()).save(any());
        verify(productSearchRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("handle(delete): productId로 ES 인덱스에서 삭제한다")
    void handle_delete_deletesFromIndex() {
        // when
        listener.handle(ProductIndexEvent.delete(10L));

        // then
        verify(productSearchRepository).deleteById(10L);
        verify(productRepository, never()).findById(any());
    }
}
