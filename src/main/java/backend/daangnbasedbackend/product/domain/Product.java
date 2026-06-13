package backend.daangnbasedbackend.product.domain;

import backend.daangnbasedbackend.global.domain.AbstractEntity;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(
    name = "products",
    indexes = {
        @Index(
            name = "idx_products_feed",
            columnList = "location, is_deleted, state, refreshed_at DESC, id DESC"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AbstractEntity {

    private static final long MAX_REFRESH_COUNT = 5L;
    private static final long REFRESH_COOLDOWN_HOURS = 24L;
    @Column(nullable = false)
    private Long sellerId;

    private Long buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    private String title;

    private String description;

    private BigDecimal price;

    private String location;

    @Enumerated(EnumType.STRING)
    private ProductState state;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "refresh_count")
    private Long refreshCount = 0L;

    @Column(name = "refreshed_at")
    private LocalDateTime refreshAt;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Column(name = "transacted_at")
    private LocalDateTime transactedAt;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    private Product(Long sellerId, ProductCategory category, String title, String description, BigDecimal price, String location) {
        this.sellerId = sellerId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.state = ProductState.ON_SALE;
        this.refreshAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static Product create(Long sellerId, ProductCategory category, String title, String description, BigDecimal price, String location, List<String> imageUrls) {
        Product product = new Product(sellerId, category, title, description, price, location);
        product.buildImages(imageUrls);
        return product;
    }

    public String getThumbnailUrl() {
        if (images == null || images.isEmpty()) {
            return "default-thumbnail-url";
        }
        return images.get(0).getImageUrl();
    }

    public void changeState(ProductState state) {
        this.state = state;
    }

    public void assignBuyer(Long buyerId) {
        this.buyerId = buyerId;
        this.transactedAt = LocalDateTime.now();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updateProduct(ProductCategory category, String title, String description, BigDecimal price, String location, List<String> imageUrls) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.state = ProductState.ON_SALE;
        this.refreshAt = LocalDateTime.now(ZoneOffset.UTC);
        this.images.clear();
        buildImages(imageUrls);
    }

    public void cancelReservation() {
        if (this.state != ProductState.RESERVED) {
            throw new ProductException(ProductErrorType.INVALID_STATE_MODIFICATION);
        }
        this.state = ProductState.ON_SALE;
        this.buyerId = null;
    }

    public void refresh() {
        if (this.refreshCount >= MAX_REFRESH_COUNT) {
            throw new ProductException(ProductErrorType.REFRESH_LIMIT_EXCEEDED);
        }
        if (this.refreshCount > 0 && this.refreshAt.isAfter(LocalDateTime.now(ZoneOffset.UTC).minusHours(REFRESH_COOLDOWN_HOURS))) {
            throw new ProductException(ProductErrorType.REFRESH_COOLDOWN_NOT_ELAPSED);
        }
        this.refreshAt = LocalDateTime.now(ZoneOffset.UTC);
        this.refreshCount++;
    }

    private void buildImages(List<String> imageUrls) {
        if (imageUrls == null) return;
        imageUrls.forEach(url -> this.images.add(ProductImage.create(this, url)));
    }
}
