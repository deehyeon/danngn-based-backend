package backend.daangnbasedbackend.product.application.dto;

import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSummaryRes(
        Long productId,
        Long sellerId,
        String categoryName,
        String title,
        BigDecimal price,
        String location,
        String thumbnailUrl,
        ProductState state,
        Long likeCount,
        LocalDateTime refreshAt,
        LocalDateTime transactedAt
) {
    public static ProductSummaryRes from(Product product) {
        return new ProductSummaryRes(
                product.getId(),
                product.getSellerId(),
                product.getCategory().getName(),
                product.getTitle(),
                product.getPrice(),
                product.getLocation(),
                product.getThumbnailUrl(),
                product.getState(),
                product.getLikeCount(),
                product.getRefreshAt(),
                product.getTransactedAt()
        );
    }
}
