package backend.daangnbasedbackend.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private static final Long SELLER_ID = 1L;
    private static final ProductCategory CATEGORY = ProductCategory.create("디지털기기");

    // ==========================================
    // create
    // ==========================================

    @Test
    @DisplayName("create: 초기 상태는 ON_SALE이고 카운트는 모두 0이다")
    void create_initialStateIsOnSaleAndCountsAreZero() {
        Product product = Product.create(SELLER_ID, CATEGORY, "아이폰 14", "상태 좋음", BigDecimal.valueOf(500000), "서울 강남구", List.of());

        assertThat(product.getState()).isEqualTo(ProductState.ON_SALE);
        assertThat(product.getViewCount()).isEqualTo(0L);
        assertThat(product.getLikeCount()).isEqualTo(0L);
        assertThat(product.getRefreshCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("create: 전달한 필드가 올바르게 저장된다")
    void create_fieldsStoredCorrectly() {
        BigDecimal price = BigDecimal.valueOf(500000);

        Product product = Product.create(SELLER_ID, CATEGORY, "아이폰 14", "상태 좋음", price, "서울 강남구", List.of());

        assertThat(product.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(product.getCategory()).isEqualTo(CATEGORY);
        assertThat(product.getTitle()).isEqualTo("아이폰 14");
        assertThat(product.getDescription()).isEqualTo("상태 좋음");
        assertThat(product.getPrice()).isEqualByComparingTo(price);
        assertThat(product.getLocation()).isEqualTo("서울 강남구");
        assertThat(product.getBuyerId()).isNull();
    }

    @Test
    @DisplayName("create: 이미지 URL로 ProductImage가 생성된다")
    void create_buildsImagesFromUrls() {
        List<String> imageUrls = List.of("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg");

        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", imageUrls);

        assertThat(product.getImages()).hasSize(2);
        assertThat(product.getImages().get(0).getImageUrl()).isEqualTo("https://cdn.example.com/1.jpg");
        assertThat(product.getImages().get(1).getImageUrl()).isEqualTo("https://cdn.example.com/2.jpg");
    }

    @Test
    @DisplayName("create: refreshAt이 현재 시각으로 설정된다")
    void create_refreshAtIsSetToNow() {
        LocalDateTime before = LocalDateTime.now();

        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());

        assertThat(product.getRefreshAt()).isAfterOrEqualTo(before);
        assertThat(product.getRefreshAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ==========================================
    // getThumbnailUrl
    // ==========================================

    @Test
    @DisplayName("getThumbnailUrl: 이미지가 있으면 첫 번째 이미지 URL을 반환한다")
    void getThumbnailUrl_returnsFirstImageUrl() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울",
                List.of("https://cdn.example.com/first.jpg", "https://cdn.example.com/second.jpg"));

        assertThat(product.getThumbnailUrl()).isEqualTo("https://cdn.example.com/first.jpg");
    }

    @Test
    @DisplayName("getThumbnailUrl: 이미지가 없으면 기본 URL을 반환한다")
    void getThumbnailUrl_returnsDefaultWhenNoImages() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());

        assertThat(product.getThumbnailUrl()).isEqualTo("default-thumbnail-url");
    }

    // ==========================================
    // changeState
    // ==========================================

    @Test
    @DisplayName("changeState: 상태가 RESERVED로 변경된다")
    void changeState_toReserved() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());

        product.changeState(ProductState.RESERVED);

        assertThat(product.getState()).isEqualTo(ProductState.RESERVED);
    }

    @Test
    @DisplayName("changeState: 상태가 SOLD_OUT으로 변경된다")
    void changeState_toSoldOut() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());

        product.changeState(ProductState.SOLD_OUT);

        assertThat(product.getState()).isEqualTo(ProductState.SOLD_OUT);
    }

    // ==========================================
    // assignBuyer
    // ==========================================

    @Test
    @DisplayName("assignBuyer: 구매자 ID와 거래 날짜가 설정된다")
    void assignBuyer_setsBuyerIdAndTransactedAt() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());
        LocalDateTime before = LocalDateTime.now();

        product.assignBuyer(2L);

        assertThat(product.getBuyerId()).isEqualTo(2L);
        assertThat(product.getTransactedAt()).isAfterOrEqualTo(before);
    }

    // ==========================================
    // updateProduct
    // ==========================================

    @Test
    @DisplayName("updateProduct: 필드가 업데이트되고 상태가 ON_SALE로 재설정된다")
    void updateProduct_updatesFieldsAndResetsState() {
        Product product = Product.create(SELLER_ID, CATEGORY, "기존 제목", "기존 설명", BigDecimal.valueOf(10000), "서울", List.of());
        product.changeState(ProductState.RESERVED);
        ProductCategory newCategory = ProductCategory.create("가구/인테리어");

        product.updateProduct(newCategory, "새 제목", "새 설명", BigDecimal.valueOf(20000), "경기 성남시", List.of());

        assertThat(product.getCategory()).isEqualTo(newCategory);
        assertThat(product.getTitle()).isEqualTo("새 제목");
        assertThat(product.getDescription()).isEqualTo("새 설명");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(product.getLocation()).isEqualTo("경기 성남시");
        assertThat(product.getState()).isEqualTo(ProductState.ON_SALE);
    }

    @Test
    @DisplayName("updateProduct: 이미지 목록이 교체된다")
    void updateProduct_replacesImages() {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울",
                List.of("https://cdn.example.com/old.jpg"));

        product.updateProduct(CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울",
                List.of("https://cdn.example.com/new1.jpg", "https://cdn.example.com/new2.jpg"));

        assertThat(product.getImages()).hasSize(2);
        assertThat(product.getImages().get(0).getImageUrl()).isEqualTo("https://cdn.example.com/new1.jpg");
    }

    @Test
    @DisplayName("updateProduct: refreshAt이 갱신된다")
    void updateProduct_refreshesRefreshAt() throws InterruptedException {
        Product product = Product.create(SELLER_ID, CATEGORY, "제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());
        LocalDateTime originalRefreshAt = product.getRefreshAt();
        Thread.sleep(10);

        product.updateProduct(CATEGORY, "새 제목", "설명", BigDecimal.valueOf(10000), "서울", List.of());

        assertThat(product.getRefreshAt()).isAfter(originalRefreshAt);
    }
}
