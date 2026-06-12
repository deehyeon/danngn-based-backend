package backend.daangnbasedbackend.product.application;

public record ProductIndexEvent(Long productId, Action action) {
    public enum Action { UPSERT, DELETE }

    public static ProductIndexEvent upsert(Long productId) {
        return new ProductIndexEvent(productId, Action.UPSERT);
    }

    public static ProductIndexEvent delete(Long productId) {
        return new ProductIndexEvent(productId, Action.DELETE);
    }
}
