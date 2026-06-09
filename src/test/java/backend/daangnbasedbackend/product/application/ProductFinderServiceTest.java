package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import backend.daangnbasedbackend.product.domain.ProductState;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFinderServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;

    private ProductFinderService productFinderService;

    @BeforeEach
    void setUp() {
        productFinderService = new ProductFinderService(productRepository, favoriteProductRepository);
    }

    private Product product(Long id) {
        Product p = Product.create(1L, ProductCategory.create("디지털기기"), "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("findById: 존재하는 상품을 반환한다")
    void findById_returnsProduct() {
        // given
        Product product = product(10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // when
        ProductRes result = productFinderService.findById(10L);

        // then
        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("아이폰");
    }

    @Test
    @DisplayName("findById: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void findById_notFound_throwsException() {
        // given
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productFinderService.findById(10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("findById: 삭제된 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void findById_deletedProduct_throwsException() {
        // given
        Product deleted = product(10L);
        deleted.softDelete();
        when(productRepository.findById(10L)).thenReturn(Optional.of(deleted));

        // when & then
        assertThatThrownBy(() -> productFinderService.findById(10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("findProducts: 삭제되지 않은 상품 목록을 반환한다")
    void findProducts_returnsActivePage() {
        // given
        Page<Product> page = new PageImpl<>(List.of(product(1L), product(2L)));
        when(productRepository.findByIsDeletedFalse(PageRequest.of(0, 10))).thenReturn(page);

        // when
        Page<ProductSummaryRes> result = productFinderService.findProducts(PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("아이폰");
    }

    @Test
    @DisplayName("findProductsBySellerId: 판매자 ID와 상태로 목록을 반환한다")
    void findProductsBySellerId_returnsFilteredPage() {
        // given
        Page<Product> page = new PageImpl<>(List.of(product(1L)));
        when(productRepository.findBySellerIdAndStateAndIsDeletedFalse(1L, ProductState.ON_SALE, PageRequest.of(0, 10)))
                .thenReturn(page);

        // when
        Page<ProductSummaryRes> result = productFinderService.findProductsBySellerId(1L, ProductState.ON_SALE, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }


    @Test
    @DisplayName("findProductsByBuyerId: 구매자 ID로 구매 목록을 반환한다")
    void findProductsByBuyerId_returnsPage() {
        // given
        Page<Product> page = new PageImpl<>(List.of(product(1L)));
        when(productRepository.findByBuyerIdAndIsDeletedFalse(2L, PageRequest.of(0, 10))).thenReturn(page);

        // when
        Page<ProductSummaryRes> result = productFinderService.findProductsByBuyerId(2L, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findFavoriteProducts: 좋아요한 상품 목록을 반환한다")
    void findFavoriteProducts_returnsPage() {
        // given
        Page<Product> page = new PageImpl<>(List.of(product(1L), product(2L)));
        when(productRepository.findFavoritesByMemberId(3L, PageRequest.of(0, 10))).thenReturn(page);

        // when
        Page<ProductSummaryRes> result = productFinderService.findFavoriteProducts(3L, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("isFavorited: DB에 찜 데이터가 있으면 true를 반환한다")
    void isFavorited_inDb_returnsTrue() {
        // given
        when(favoriteProductRepository.existsByMemberIdAndProductId(2L, 10L)).thenReturn(true);

        // when & then
        assertThat(productFinderService.isFavorited(2L, 10L)).isTrue();
    }

    @Test
    @DisplayName("isFavorited: DB에 찜 데이터가 없으면 false를 반환한다")
    void isFavorited_notInDb_returnsFalse() {
        // given
        when(favoriteProductRepository.existsByMemberIdAndProductId(2L, 10L)).thenReturn(false);

        // when & then
        assertThat(productFinderService.isFavorited(2L, 10L)).isFalse();
    }
}