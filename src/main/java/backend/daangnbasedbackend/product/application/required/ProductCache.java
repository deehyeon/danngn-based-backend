package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductCache {
    private final MemoryMap memoryMap;

    private static final String FEED_KEY_PREFIX = "town:feed:";
    private static final long MAX_FEED_CACHE_SIZE = 1000;

    public void addToFeedCache(String location, Long productId, long score) {
        String key = FEED_KEY_PREFIX + location;
        memoryMap.addToSortedSet(key, productId.toString(), score);
        memoryMap.trimSortedSet(key, MAX_FEED_CACHE_SIZE);
    }

    public void removeFromFeedCache(String location, Long productId) {
        memoryMap.removeFromSortedSet(FEED_KEY_PREFIX + location, productId.toString());
    }

    public List<Long> getTopProductIds(String location, double maxScore, int count) {
        return memoryMap.reverseRangeFromSortedSetByScore(FEED_KEY_PREFIX + location, maxScore, count)
                .stream()
                .map(Long::parseLong)
                .toList();
    }
}
