package backend.daangnbasedbackend.product.application.event;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record ProductCacheEvent(Long productId, Action action, String location, Long score) {
    public enum Action { ADD, REMOVE }

    public static ProductCacheEvent add(Long productId, String location, LocalDateTime refreshAt) {
        return new ProductCacheEvent(productId, Action.ADD, location, refreshAt.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    public static ProductCacheEvent remove(Long productId, String location) {
        return new ProductCacheEvent(productId, Action.REMOVE, location, null);
    }
}
