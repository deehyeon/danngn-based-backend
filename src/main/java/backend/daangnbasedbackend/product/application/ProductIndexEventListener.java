package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductSearchRepository;
import backend.daangnbasedbackend.product.domain.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexEventListener {
    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductIndexEvent event) {
        switch (event.action()) {
            case DELETE -> {
                productSearchRepository.deleteById(event.productId());
                log.info("ES 인덱스 삭제 완료: productId={}", event.productId());
            }
            case UPSERT -> productRepository.findById(event.productId()).ifPresentOrElse(
                product -> {
                    if (product.getIsDeleted()) {
                        productSearchRepository.deleteById(product.getId());
                        log.info("소프트삭제 상품 ES 인덱스 제거: productId={}", product.getId());
                    } else {
                        productSearchRepository.save(ProductDocument.from(product));
                        log.info("ES 인덱스 저장/갱신 완료: productId={}", product.getId());
                    }
                },
                () -> log.warn("ES 동기화 실패 — 상품 없음: productId={}", event.productId())
            );
        }
    }
}
