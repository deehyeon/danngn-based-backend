package backend.daangnbasedbackend.product.application.dto;

import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductImage;
import backend.daangnbasedbackend.product.domain.ProductState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductRes(
        Long productId,
        Long sellerId,
        Long buyerId,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        String location,
        ProductState state,
        Long viewCount,
        Long likeCount,
        LocalDateTime refreshAt,
        LocalDateTime transactedAt,
        List<String> imageUrls
) {
    public static ProductRes from(Product product) {
        return new ProductRes(
                product.getId(),
                product.getSellerId(),
                product.getBuyerId(),
                product.getCategory().getName(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getLocation(),
                product.getState(),
                product.getViewCount(),
                product.getLikeCount(),
                product.getRefreshAt(),
                product.getTransactedAt(),
                product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .toList()
        );
    }
}
