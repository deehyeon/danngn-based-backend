package backend.daangnbasedbackend.product.application.required;

import java.util.Map;

public interface FavoriteCacheRepository {

    void recordLikeChange(Long productId, long change);

    long getPendingLikeChange(Long productId);

    Map<Long, Long> flushPendingLikeChanges();
}
