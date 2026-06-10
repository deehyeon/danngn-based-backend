package backend.daangnbasedbackend.product.domain;

import backend.daangnbasedbackend.global.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "favorite_products",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_favorite_member_product",
            columnNames = {"member_id", "product_id"}
        )
    }
)
public class FavoriteProduct extends AbstractEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private FavoriteProduct(Long memberId, Long productId) {
        this.memberId = memberId;
        this.productId = productId;
    }

    public static FavoriteProduct create(Long memberId, Long productId) {
        return new FavoriteProduct(memberId, productId);
    }
}