package backend.daangnbasedbackend.product.domain;

import backend.daangnbasedbackend.global.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory extends AbstractEntity {

    @Column(nullable = false)
    private String name;

    private ProductCategory(String name) {
        this.name = name;
    }

    public static ProductCategory create(String name) {
        return new ProductCategory(name);
    }
}
