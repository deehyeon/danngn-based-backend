package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.auth.application.KakaoOAuthClient;
import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import backend.daangnbasedbackend.product.application.provided.FavoriteWriter;
import backend.daangnbasedbackend.product.application.required.FavoriteCacheRepository;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductSearchRepository;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Redisson 분산 락 통합 테스트
 *
 * 사전 조건: Redis 실행 필요 — docker-compose up -d redis
 *
 * 검증 대상: 동일한 (memberId, productId) 조합에 대한 동시 요청이
 *           Redis 분산 락에 의해 직렬화되는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class FavoriteDistributedLockIntegrationTest {

    // Spring 프록시를 통해 주입 — @DistributedLock AOP가 적용된 상태
    @Autowired private FavoriteWriter favoriteWriter;

    // 비즈니스 로직 의존성 모킹 (DB/캐시 불필요)
    @MockitoBean private ProductRepository productRepository;
    @MockitoBean private FavoriteProductRepository favoriteProductRepository;
    @MockitoBean private FavoriteCacheRepository favoriteCacheRepository;

    // 외부 인프라 모킹 (컨텍스트 로딩에만 필요)
    @MockitoBean private MemoryMap memoryMap;
    @MockitoBean private KakaoOAuthClient kakaoOAuthClient;
    @MockitoBean private ElasticsearchOperations elasticsearchOperations;
    @MockitoBean private ProductSearchRepository productSearchRepository;

    @Test
    @DisplayName("동일 (memberId, productId) 동시 요청 5건 — Redisson 락으로 직렬화되어 최대 1건만 동시 실행된다")
    void 동시_좋아요_요청_분산락으로_직렬화된다() throws InterruptedException {
        // given
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        Product product = Product.create(1L, ProductCategory.create("디지털기기"),
                "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(product, "id", 10L);

        // 50ms 지연: 락이 없으면 여러 스레드가 동시에 진입 → maxConcurrent > 1
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenAnswer(inv -> {
            int cur = currentConcurrent.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, cur));
            Thread.sleep(50);
            currentConcurrent.decrementAndGet();
            return Optional.of(product);
        });
        when(favoriteProductRepository.findByMemberIdAndProductId(any(), eq(10L)))
                .thenReturn(Optional.empty());
        when(favoriteProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(favoriteCacheRepository.getPendingLikeChange(10L)).thenReturn(0L);

        // when — 5개 스레드가 동시에 동일한 락 키(favorite:member:1:product:10)로 경쟁
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    favoriteWriter.toggleFavorite(1L, 10L);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();           // 모든 스레드 준비 완료
        start.countDown();       // 동시 출발
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(errors).as("모든 요청이 예외 없이 완료되어야 한다").isEmpty();
        assertThat(maxConcurrent.get())
                .as("Redisson 락이 직렬화를 보장하므로 동시 실행 수는 최대 1이어야 한다")
                .isEqualTo(1);
    }
}
