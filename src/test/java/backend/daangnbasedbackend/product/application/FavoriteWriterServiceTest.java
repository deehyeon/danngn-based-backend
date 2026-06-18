package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes;
import backend.daangnbasedbackend.product.application.required.FavoriteCache;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteWriterServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private FavoriteCache favoriteCache;

    private FavoriteWriterService favoriteWriterService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        favoriteWriterService = new FavoriteWriterService(
                productRepository, favoriteProductRepository, favoriteCache);
    }

    private Product activeProduct(long likeCount) {
        Product product = Product.create(1L, ProductCategory.create("디지털기기"), "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(product, "likeCount", likeCount);
        return product;
    }

    private FavoriteProduct activeFavorite() {
        return FavoriteProduct.create(MEMBER_ID, PRODUCT_ID);
    }

    private FavoriteProduct softDeletedFavorite() {
        FavoriteProduct fav = FavoriteProduct.create(MEMBER_ID, PRODUCT_ID);
        fav.softDelete();
        return fav;
    }

    // ==========================================
    // toggleFavorite — 신규 찜 추가 (레코드 없음)
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: 찜 레코드가 없으면 신규 저장하고 isFavorited=true, likeCount+1")
    void toggleFavorite_noRecord_savesAndReturnsTrue() {
        // given
        Product product = activeProduct(5L);
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCache.getPendingLikeChange(PRODUCT_ID)).thenReturn(0L);
        when(favoriteProductRepository.findByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        when(favoriteProductRepository.save(any(FavoriteProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.favorited()).isTrue();
        assertThat(result.likeCount()).isEqualTo(6L);
        verify(favoriteProductRepository).save(any(FavoriteProduct.class));
        verify(favoriteCache).recordLikeChange(PRODUCT_ID, 1L);
    }

    // ==========================================
    // toggleFavorite — 찜 취소 (활성 레코드 소프트 삭제)
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: 활성 찜 레코드가 있으면 소프트 삭제하고 isFavorited=false, likeCount-1")
    void toggleFavorite_activeFavorite_softDeletesAndReturnsFalse() {
        // given
        Product product = activeProduct(5L);
        FavoriteProduct activeFav = activeFavorite();
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCache.getPendingLikeChange(PRODUCT_ID)).thenReturn(0L);
        when(favoriteProductRepository.findByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(Optional.of(activeFav));

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.favorited()).isFalse();
        assertThat(result.likeCount()).isEqualTo(4L);
        assertThat(activeFav.getIsDeleted()).isTrue();
        verify(favoriteProductRepository, never()).save(any());
        verify(favoriteCache).recordLikeChange(PRODUCT_ID, -1L);
    }

    // ==========================================
    // toggleFavorite — 재찜 (소프트 삭제 레코드 복원)
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: 소프트 삭제된 레코드가 있으면 복원하고 isFavorited=true, likeCount+1")
    void toggleFavorite_softDeletedFavorite_restoresAndReturnsTrue() {
        // given
        Product product = activeProduct(5L);
        FavoriteProduct softDeletedFav = softDeletedFavorite();
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCache.getPendingLikeChange(PRODUCT_ID)).thenReturn(0L);
        when(favoriteProductRepository.findByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(Optional.of(softDeletedFav));

        // when
        ToggleFavoriteRes result = favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID);

        // then
        assertThat(result.favorited()).isTrue();
        assertThat(result.likeCount()).isEqualTo(6L);
        assertThat(softDeletedFav.getIsDeleted()).isFalse();
        verify(favoriteProductRepository, never()).save(any());
        verify(favoriteCache).recordLikeChange(PRODUCT_ID, 1L);
    }

    // ==========================================
    // toggleFavorite — likeCount 계산
    // ==========================================

    @Test
    @DisplayName("toggleFavorite: Redis delta가 누적된 경우 likeCount에 반영된다")
    void toggleFavorite_withExistingDelta_reflectsDeltaInCount() {
        // given
        Product product = activeProduct(10L);
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCache.getPendingLikeChange(PRODUCT_ID)).thenReturn(3L);
        when(favoriteProductRepository.findByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
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
        FavoriteProduct activeFav = activeFavorite();
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteCache.getPendingLikeChange(PRODUCT_ID)).thenReturn(-1L);
        when(favoriteProductRepository.findByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).thenReturn(Optional.of(activeFav));

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
        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteWriterService.toggleFavorite(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }
}
