package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.required.FavoriteCacheRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class FavoriteSyncScheduler {
    private final FavoriteCacheRepository favoriteCacheRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "FavoriteSyncScheduler", lockAtMostFor = "PT55S", lockAtLeastFor = "PT5S")
    public void syncLikeCounts() {
        Map<Long, Long> deltas = favoriteCacheRepository.flushPendingLikeChanges();
        if (deltas.isEmpty()) {
            return;
        }

        log.info("좋아요 수 동기화 시작: {}개 상품", deltas.size());

        for (Map.Entry<Long, Long> entry : deltas.entrySet()) {
            Long productId = entry.getKey();
            long delta = entry.getValue();
            if (delta == 0) continue;
            try {
                productRepository.updateLikeCount(productId, delta);
            } catch (Exception e) {
                log.error("좋아요 수 동기화 실패: productId={}, delta={}", productId, delta, e);
            }
        }

        log.info("좋아요 수 동기화 완료");
    }
}
