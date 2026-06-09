package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
}
