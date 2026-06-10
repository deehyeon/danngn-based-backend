package backend.daangnbasedbackend.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageTest {

    @Test
    @DisplayName("ProductImage는 Product를 통해서만 생성되며 imageUrl이 올바르게 저장된다")
    void productImage_createdThroughProduct_storesImageUrl() {
        // given
        Product product = Product.create(1L, ProductCategory.create("디지털기기"),
                "제목", "설명", BigDecimal.valueOf(10000), "서울",
                List.of("https://cdn.example.com/image.jpg"));

        // when
        ProductImage image = product.getImages().get(0);

        // then
        assertThat(image.getImageUrl()).isEqualTo("https://cdn.example.com/image.jpg");
        assertThat(image.getProduct()).isEqualTo(product);
    }

    @Test
    @DisplayName("Product 생성 시 이미지 순서가 입력 순서대로 유지된다")
    void productImages_maintainInsertionOrder() {
        // given
        List<String> urls = List.of(
                "https://cdn.example.com/1.jpg",
                "https://cdn.example.com/2.jpg",
                "https://cdn.example.com/3.jpg"
        );

        // when
        Product product = Product.create(1L, ProductCategory.create("디지털기기"),
                "제목", "설명", BigDecimal.valueOf(10000), "서울", urls);

        // then
        assertThat(product.getImages()).hasSize(3);
        assertThat(product.getImages().get(0).getImageUrl()).isEqualTo("https://cdn.example.com/1.jpg");
        assertThat(product.getImages().get(1).getImageUrl()).isEqualTo("https://cdn.example.com/2.jpg");
        assertThat(product.getImages().get(2).getImageUrl()).isEqualTo("https://cdn.example.com/3.jpg");
    }
}
