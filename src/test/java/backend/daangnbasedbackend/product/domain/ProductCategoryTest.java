package backend.daangnbasedbackend.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCategoryTest {

    @Test
    @DisplayName("create: 카테고리 이름이 올바르게 저장된다")
    void create_storesCategoryName() {
        // when
        ProductCategory category = ProductCategory.create("디지털기기");

        // then
        assertThat(category.getName()).isEqualTo("디지털기기");
    }
}
