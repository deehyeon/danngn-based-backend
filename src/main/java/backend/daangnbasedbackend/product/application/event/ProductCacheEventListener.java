package backend.daangnbasedbackend.product.application.event;

import backend.daangnbasedbackend.product.application.required.ProductCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheEventListener {
    private final ProductCache productCache;
    private final RetryTemplate cacheRetryTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductCacheEvent event) {
        cacheRetryTemplate.execute(
            ctx -> { syncToCache(event); return null; },
            ctx -> {
                log.error("피드 캐시 동기화 최종 실패: productId={}, action={}", event.productId(), event.action(), ctx.getLastThrowable());
                return null;
            }
        );
    }

    private void syncToCache(ProductCacheEvent event) {
        switch (event.action()) {
            case ADD -> {
                productCache.addToFeedCache(event.location(), event.productId(), event.score());
                log.info("피드 캐시 추가 완료: productId={}, location={}", event.productId(), event.location());
            }
            case REMOVE -> {
                productCache.removeFromFeedCache(event.location(), event.productId());
                log.info("피드 캐시 제거 완료: productId={}, location={}", event.productId(), event.location());
            }
        }
    }
}
