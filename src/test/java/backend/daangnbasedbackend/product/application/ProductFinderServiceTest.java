package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.member.application.dto.MemberRes;
import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.domain.MemberRole;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import backend.daangnbasedbackend.product.application.dto.ProductCursor;
import backend.daangnbasedbackend.product.application.dto.ProductFeedRes;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFinderServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private MemberFinder memberFinder;

    private ProductFinderService productFinderService;

    @BeforeEach
    void setUp() {
        productFinderService = new ProductFinderService(productRepository, favoriteProductRepository, memberFinder);
    }

    private MemberRes memberWithLocation(String location) {
        return new MemberRes(1L, "닉네임", "test@test.com", location, 36.5, null, null, MemberRole.USER);
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
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

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
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

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
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

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
        when(favoriteProductRepository.existsByMemberIdAndProductIdAndIsDeletedFalse(2L, 10L)).thenReturn(true);

        // when & then
        assertThat(productFinderService.isFavorited(2L, 10L)).isTrue();
    }

    @Test
    @DisplayName("isFavorited: DB에 찜 데이터가 없으면 false를 반환한다")
    void isFavorited_notInDb_returnsFalse() {
        // given
        when(favoriteProductRepository.existsByMemberIdAndProductIdAndIsDeletedFalse(2L, 10L)).thenReturn(false);

        // when & then
        assertThat(productFinderService.isFavorited(2L, 10L)).isFalse();
    }

    // ==========================================
    // findFeed
    // ==========================================

    @Test
    @DisplayName("findFeed: 사용자 동네의 상품 목록을 반환한다")
    void findFeed_returnsFeedByMemberLocation() {
        // given
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        List<Product> products = List.of(product(1L), product(2L));
        when(productRepository.findFeed("서울", PageRequest.ofSize(21))).thenReturn(products);

        // when
        ProductFeedRes result = productFinderService.findFeed(1L, null, 20, null);

        // then
        assertThat(result.products()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("findFeed: size+1개가 조회되면 hasNext=true이고 nextCursor가 설정된다")
    void findFeed_hasNextPage_returnsNextCursor() {
        // given
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        List<Product> products = List.of(product(1L), product(2L), product(3L)); // size(2)+1
        when(productRepository.findFeed("서울", PageRequest.ofSize(3))).thenReturn(products);

        // when
        ProductFeedRes result = productFinderService.findFeed(1L, null, 2, null);

        // then
        assertThat(result.products()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("findFeed: state 필터가 있으면 해당 상태의 상품만 반환한다")
    void findFeed_withStateFilter_returnsFilteredProducts() {
        // given
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        List<Product> products = List.of(product(1L));
        when(productRepository.findFeedByState("서울", ProductState.ON_SALE, PageRequest.ofSize(11))).thenReturn(products);

        // when
        ProductFeedRes result = productFinderService.findFeed(1L, null, 10, ProductState.ON_SALE);

        // then
        assertThat(result.products()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("findFeed: 동네 설정이 없으면 LOCATION_NOT_SET 예외를 던진다")
    void findFeed_memberLocationIsNull_throwsException() {
        // given
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation(null));

        // when & then
        assertThatThrownBy(() -> productFinderService.findFeed(1L, null, 20, null))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.LOCATION_NOT_SET);
    }

    @Test
    @DisplayName("findFeed: 커서가 있으면 커서 이후 상품을 반환한다")
    void findFeed_withCursor_returnsCursoredProducts() {
        // given
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        LocalDateTime cursorTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        String cursor = new ProductCursor(cursorTime, 5L).encode();
        List<Product> products = List.of(product(3L));
        when(productRepository.findFeedAfterCursor("서울", cursorTime, 5L, PageRequest.ofSize(11))).thenReturn(products);

        // when
        ProductFeedRes result = productFinderService.findFeed(1L, cursor, 10, null);

        // then
        assertThat(result.products()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
    }
}