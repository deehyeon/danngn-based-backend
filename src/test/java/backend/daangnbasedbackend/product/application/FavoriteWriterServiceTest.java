package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes;
import backend.daangnbasedbackend.product.application.required.FavoriteCacheRepository;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.FavoriteProduct;
import backend.daangnbasedbackend.product.domain.Product;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteWriterServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private FavoriteCacheRepository favoriteCacheRepository;

    private FavoriteWriterService favoriteWriterService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        favoriteWriterService = new FavoriteWriterService(
                productRepository, favoriteProductRepository, favoriteCacheRepository);
    }

    private Product activeProduct(long likeCount) {
        Product product = Product.create(1L, ProductCategory.create("디지털기기"), "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(product, "likeCount", likeCount);
        return product;
    }

    // ==========================================
    // toggleFavorite — 찜 추가
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: 찜하지 않은 상품을 찜하면 isFavorited=true, likeCount+1")
    void toggleFavorite_notYetFavorited_addsAndReturnsTrue() {
        // given
        Product product = activeProduct(5L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCacheRepository.getPendingLikeChange(PRODUCT_ID)).thenReturn(0L);
        when(favoriteProductRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
        when(favoriteProductRepository.save(any(FavoriteProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.favorited()).isTrue();
        assertThat(result.likeCount()).isEqualTo(6L);
        verify(favoriteProductRepository).save(any(FavoriteProduct.class));
        verify(favoriteCacheRepository).recordLikeChange(PRODUCT_ID, 1L);
    }

    @Test
    @DisplayName("toggleFavorite: 이미 찜한 상품을 다시 누르면 isFavorited=false, likeCount-1")
    void toggleFavorite_alreadyFavorited_removesAndReturnsFalse() {
        // given
        Product product = activeProduct(5L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCacheRepository.getPendingLikeChange(PRODUCT_ID)).thenReturn(0L);
        when(favoriteProductRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(true);

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.favorited()).isFalse();
        assertThat(result.likeCount()).isEqualTo(4L);
        verify(favoriteProductRepository).deleteByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID);
        verify(favoriteCacheRepository).recordLikeChange(PRODUCT_ID, -1L);
        verify(favoriteProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleFavorite: Redis delta가 누적된 경우 likeCount에 반영된다")
    void toggleFavorite_withExistingDelta_reflectsDeltaInCount() {
        // given
        Product product = activeProduct(10L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCacheRepository.getPendingLikeChange(PRODUCT_ID)).thenReturn(3L); // 아직 DB에 미반영된 +3
        when(favoriteProductRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
        when(favoriteProductRepository.save(any(FavoriteProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.likeCount()).isEqualTo(14L); // 10 + 3 + 1
    }

    @Test
    @DisplayName("toggleFavorite: likeCount가 0 미만이 되지 않도록 보호된다")
    void toggleFavorite_likeCountCannotGoBelowZero() {
        // given
        Product product = activeProduct(0L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCacheRepository.getPendingLikeChange(PRODUCT_ID)).thenReturn(-1L); // 비정상 상태 시뮬레이션
        when(favoriteProductRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(true);

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.likeCount()).isEqualTo(0L); // Math.max(0, 0 + (-1) + (-1)) = 0
    }

    // ==========================================
    // toggleFavorite — 예외
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: 존재하지 않는 상품이면 PRODUCT_NOT_FOUND 예외를 던진다")
    void toggleFavorite_productNotFound_throwsException() {
        // given
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("toggleFavorite: 삭제된 상품이면 PRODUCT_NOT_FOUND 예외를 던진다")
    void toggleFavorite_deletedProduct_throwsException() {
        // given
        Product deleted = activeProduct(0L);
        deleted.softDelete();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(deleted));

        // when & then
        assertThatThrownBy(() -> favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }
}
