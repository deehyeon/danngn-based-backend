package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ProductCategoryRes;
import backend.daangnbasedbackend.product.application.required.ProductCategoryRepository;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryFinderServiceTest {
    @Mock private ProductCategoryRepository productCategoryRepository;

    private ProductCategoryFinderService productCategoryFinderService;

    @BeforeEach
    void setUp() {
        productCategoryFinderService = new ProductCategoryFinderService(productCategoryRepository);
    }

    private ProductCategory category(Long id, String name) {
        ProductCategory c = ProductCategory.create(name);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    // ==========================================
    // findById
    // ==========================================

    @Test
    @DisplayName("findById: 카테고리를 DTO로 반환한다")
    void findById_returnsCategoryRes() {
        // given
        ProductCategory category = category(1L, "디지털기기");
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // when
        ProductCategoryRes result = productCategoryFinderService.findById(1L);

        // then
        assertThat(result.categoryId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("디지털기기");
    }

    @Test
    @DisplayName("findById: 존재하지 않는 카테고리 — PRODUCT_CATEGORY_NOT_FOUND 예외를 던진다")
    void findById_notFound_throwsException() {
        // given
        when(productCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productCategoryFinderService.findById(99L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_CATEGORY_NOT_FOUND);
    }

    // ==========================================
    // findAll
    // ==========================================

    @Test
    @DisplayName("findAll: 전체 카테고리 목록을 DTO로 반환한다")
    void findAll_returnsAllCategories() {
        // given
        List<ProductCategory> categories = List.of(
                category(1L, "디지털기기"),
                category(2L, "가구/인테리어"),
                category(3L, "의류")
        );
        when(productCategoryRepository.findAll()).thenReturn(categories);

        // when
        List<ProductCategoryRes> result = productCategoryFinderService.findAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("디지털기기");
        assertThat(result.get(1).name()).isEqualTo("가구/인테리어");
        assertThat(result.get(2).name()).isEqualTo("의류");
    }

    @Test
    @DisplayName("findAll: 카테고리가 없으면 빈 목록을 반환한다")
    void findAll_empty_returnsEmptyList() {
        // given
        when(productCategoryRepository.findAll()).thenReturn(List.of());

        // when
        List<ProductCategoryRes> result = productCategoryFinderService.findAll();

        // then
        assertThat(result).isEmpty();
    }
}
