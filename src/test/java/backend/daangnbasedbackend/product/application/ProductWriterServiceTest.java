package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.member.application.dto.MemberRes;
import backend.daangnbasedbackend.member.application.provided.MemberFinder;
import backend.daangnbasedbackend.member.domain.MemberRole;
import backend.daangnbasedbackend.member.exception.MemberErrorType;
import backend.daangnbasedbackend.member.exception.MemberException;
import backend.daangnbasedbackend.product.application.dto.ProductCreateReq;
import backend.daangnbasedbackend.product.application.dto.ProductUpdateReq;
import backend.daangnbasedbackend.product.application.required.ProductCategoryRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWriterServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private MemberFinder memberFinder;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ProductWriterService productWriterService;

    @BeforeEach
    void setUp() {
        productWriterService = new ProductWriterService(productRepository, productCategoryRepository, memberFinder, eventPublisher);
    }

    private MemberRes memberWithLocation(String location) {
        return new MemberRes(1L, "닉네임", "test@test.com", location, 36.5, null, null, MemberRole.USER);
    }

    private Product activeProduct() {
        Product product = Product.create(1L, ProductCategory.create("디지털기기"), "아이폰", "설명", BigDecimal.valueOf(500000), "서울", List.of());
        ReflectionTestUtils.setField(product, "id", 10L);
        return product;
    }

    // ==========================================
    // create
    // ==========================================

    @Test
    @DisplayName("create: 상품이 저장되고 ID를 반환한다")
    void create_savesProductAndReturnsId() {
        // given
        ProductCreateReq req = new ProductCreateReq(1L, "아이폰", "설명", BigDecimal.valueOf(500000), List.of("img.jpg"));
        Product saved = activeProduct();
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(ProductCategory.create("디지털기기")));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        // when
        Long id = productWriterService.create(1L, req);

        // then
        assertThat(id).isEqualTo(10L);
        verify(productRepository).save(any(Product.class));
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("create: 동네 미설정 회원이 등록하면 LOCATION_NOT_SET 예외를 던진다")
    void create_memberLocationNotSet_throwsException() {
        // given
        ProductCreateReq req = new ProductCreateReq(1L, "아이폰", "설명", BigDecimal.valueOf(500000), null);
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation(null));

        // when & then
        assertThatThrownBy(() -> productWriterService.create(1L, req))
                .isInstanceOf(MemberException.class)
                .extracting("errorType")
                .isEqualTo(MemberErrorType.LOCATION_NOT_SET);
    }

    @Test
    @DisplayName("create: 존재하지 않는 카테고리 — PRODUCT_CATEGORY_NOT_FOUND 예외를 던진다")
    void create_categoryNotFound_throwsException() {
        // given
        ProductCreateReq req = new ProductCreateReq(99L, "아이폰", "설명", BigDecimal.valueOf(500000), null);
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("서울"));
        when(productCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.create(1L, req))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_CATEGORY_NOT_FOUND);
    }

    // ==========================================
    // update
    // ==========================================

    @Test
    @DisplayName("update: 상품 필드가 수정되고 location은 회원 동네로 갱신된다")
    void update_updatesProductFields() {
        // given
        Product product = activeProduct();
        ProductUpdateReq req = new ProductUpdateReq(2L, "새 제목", "새 설명", BigDecimal.valueOf(300000), List.of());
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));
        when(memberFinder.findById(1L)).thenReturn(memberWithLocation("경기 성남시"));
        when(productCategoryRepository.findById(2L)).thenReturn(Optional.of(ProductCategory.create("가구/인테리어")));

        // when
        productWriterService.update(1L, 10L, req);

        // then
        assertThat(product.getTitle()).isEqualTo("새 제목");
        assertThat(product.getLocation()).isEqualTo("경기 성남시");
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("update: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void update_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        ProductUpdateReq req = new ProductUpdateReq(1L, "제목", "설명", BigDecimal.valueOf(10000), List.of());
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.update(999L, 10L, req))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("update: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void update_productNotFound_throwsException() {
        // given
        ProductUpdateReq req = new ProductUpdateReq(1L, "제목", "설명", BigDecimal.valueOf(10000), List.of());
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.update(1L, 10L, req))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("update: 탈퇴·삭제된 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void update_deletedProduct_throwsException() {
        // given
        ProductUpdateReq req = new ProductUpdateReq(1L, "제목", "설명", BigDecimal.valueOf(10000), List.of());
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.update(1L, 10L, req))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    // ==========================================
    // delete
    // ==========================================

    @Test
    @DisplayName("delete: 상품이 소프트 삭제된다")
    void delete_softDeletesProduct() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.delete(1L, 10L);

        // then
        assertThat(product.getIsDeleted()).isTrue();
        verify(eventPublisher).publishEvent(ProductIndexEvent.delete(10L));
    }

    @Test
    @DisplayName("delete: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void delete_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.delete(999L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("delete: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void delete_productNotFound_throwsException() {
        // given
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.delete(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    // ==========================================
    // refresh
    // ==========================================

    @Test
    @DisplayName("refresh: refreshCount가 증가한다")
    void refresh_incrementsRefreshCount() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.refresh(1L, 10L);

        // then
        assertThat(product.getRefreshCount()).isEqualTo(1L);
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("refresh: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void refresh_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.refresh(999L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("refresh: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void refresh_productNotFound_throwsException() {
        // given
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.refresh(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("refresh: 끌어올리기 횟수가 5회면 REFRESH_LIMIT_EXCEEDED 예외를 던진다")
    void refresh_exceedsMaxCount_throwsException() {
        // given
        Product product = activeProduct();
        ReflectionTestUtils.setField(product, "refreshCount", 5L);
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.refresh(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.REFRESH_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("refresh: 이전 끌어올리기로부터 24시간이 경과하지 않으면 REFRESH_COOLDOWN_NOT_ELAPSED 예외를 던진다")
    void refresh_within24Hours_throwsException() {
        // given
        Product product = activeProduct();
        ReflectionTestUtils.setField(product, "refreshCount", 1L);
        ReflectionTestUtils.setField(product, "refreshAt", LocalDateTime.now().minusHours(1));
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.refresh(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.REFRESH_COOLDOWN_NOT_ELAPSED);
    }

    @Test
    @DisplayName("refresh: 이전 끌어올리기로부터 24시간이 경과하면 성공한다")
    void refresh_after24Hours_succeeds() {
        // given
        Product product = activeProduct();
        ReflectionTestUtils.setField(product, "refreshCount", 1L);
        ReflectionTestUtils.setField(product, "refreshAt", LocalDateTime.now().minusHours(25));
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.refresh(1L, 10L);

        // then
        assertThat(product.getRefreshCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("refresh: 첫 번째 끌어올리기(refreshCount=0)는 쿨다운 없이 바로 가능하다")
    void refresh_firstRefresh_noCooldown() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.refresh(1L, 10L);

        // then
        assertThat(product.getRefreshCount()).isEqualTo(1L);
    }

    // ==========================================
    // reserve
    // ==========================================

    @Test
    @DisplayName("reserve: 상태가 RESERVED로 변경되고 구매자가 할당된다")
    void reserve_changesStateToReservedAndAssignsBuyer() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.reserve(1L, 10L, 2L);

        // then
        assertThat(product.getState()).isEqualTo(ProductState.RESERVED);
        assertThat(product.getBuyerId()).isEqualTo(2L);
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("reserve: 판매자와 구매자가 동일하면 INVALID_TRADE_REQUEST 예외를 던진다")
    void reserve_selfTrade_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.reserve(1L, 10L, 1L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.INVALID_TRADE_REQUEST);
    }

    @Test
    @DisplayName("reserve: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void reserve_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.reserve(999L, 10L, 2L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("reserve: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void reserve_productNotFound_throwsException() {
        // given
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.reserve(1L, 10L, 2L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    // ==========================================
    // completeTrade
    // ==========================================

    @Test
    @DisplayName("completeTrade: 상태가 SOLD_OUT으로 변경되고 구매자가 할당된다")
    void completeTrade_changesStateToSoldOutAndAssignsBuyer() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.completeTrade(1L, 10L, 2L);

        // then
        assertThat(product.getState()).isEqualTo(ProductState.SOLD_OUT);
        assertThat(product.getBuyerId()).isEqualTo(2L);
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("completeTrade: 판매자와 구매자가 동일하면 INVALID_TRADE_REQUEST 예외를 던진다")
    void completeTrade_selfTrade_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.completeTrade(1L, 10L, 1L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.INVALID_TRADE_REQUEST);
    }

    @Test
    @DisplayName("completeTrade: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void completeTrade_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.completeTrade(999L, 10L, 2L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("completeTrade: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void completeTrade_productNotFound_throwsException() {
        // given
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.completeTrade(1L, 10L, 2L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }

    // ==========================================
    // cancelReservation
    // ==========================================

    @Test
    @DisplayName("cancelReservation: 상태가 ON_SALE로 돌아오고 구매자가 해제된다")
    void cancelReservation_changesStateToOnSaleAndClearsBuyer() {
        // given
        Product product = activeProduct();
        product.changeState(ProductState.RESERVED);
        product.assignBuyer(2L);
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when
        productWriterService.cancelReservation(1L, 10L);

        // then
        assertThat(product.getState()).isEqualTo(ProductState.ON_SALE);
        assertThat(product.getBuyerId()).isNull();
        verify(eventPublisher).publishEvent(ProductIndexEvent.upsert(10L));
    }

    @Test
    @DisplayName("cancelReservation: 예약 중이 아닌 상태에서 취소 시 INVALID_STATE_MODIFICATION 예외를 던진다")
    void cancelReservation_notReserved_throwsException() {
        // given
        Product product = activeProduct();
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.cancelReservation(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.INVALID_STATE_MODIFICATION);
    }

    @Test
    @DisplayName("cancelReservation: 본인 상품이 아니면 UNAUTHORIZED_ACCESS 예외를 던진다")
    void cancelReservation_notOwner_throwsException() {
        // given
        Product product = activeProduct();
        product.changeState(ProductState.RESERVED);
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productWriterService.cancelReservation(999L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("cancelReservation: 존재하지 않는 상품 — PRODUCT_NOT_FOUND 예외를 던진다")
    void cancelReservation_productNotFound_throwsException() {
        // given
        when(productRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productWriterService.cancelReservation(1L, 10L))
                .isInstanceOf(ProductException.class)
                .extracting("errorType")
                .isEqualTo(ProductErrorType.PRODUCT_NOT_FOUND);
    }
}
