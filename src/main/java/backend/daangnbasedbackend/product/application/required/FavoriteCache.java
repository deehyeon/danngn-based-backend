package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FavoriteCache {
    private final MemoryMap memoryMap;

    private static final String PENDING_LIKE_COUNT_PREFIX = "FAV:PENDING_LIKE_COUNT:";
    private static final String PENDING_SYNC_PRODUCTS_KEY = "FAV:PENDING_SYNC_PRODUCTS";

    public void recordLikeChange(Long productId, long change) {
        memoryMap.increment(PENDING_LIKE_COUNT_PREFIX + productId, change);
        memoryMap.addToSet(PENDING_SYNC_PRODUCTS_KEY, productId.toString());
    }

    public long getPendingLikeChange(Long productId) {
        String value = memoryMap.getValue(PENDING_LIKE_COUNT_PREFIX + productId);
        return value == null ? 0L : Long.parseLong(value);
    }

    public Map<Long, Long> flushPendingLikeChanges() {
        Set<String> productIds = memoryMap.popAllFromSet(PENDING_SYNC_PRODUCTS_KEY);
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> result = new HashMap<>();
        for (String idStr : productIds) {
            Long productId = Long.parseLong(idStr);
            String deltaStr = memoryMap.getAndDelete(PENDING_LIKE_COUNT_PREFIX + productId);
            if (deltaStr != null) {
                result.put(productId, Long.parseLong(deltaStr));
            }
        }
        return result;
    }
}
