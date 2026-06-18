package backend.daangnbasedbackend.product.adapter;

import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import backend.daangnbasedbackend.global.application.security.AuthDetails;
import backend.daangnbasedbackend.product.application.provided.FavoriteWriter;
import backend.daangnbasedbackend.product.application.required.FavoriteCache;
import backend.daangnbasedbackend.product.application.dto.ProductCategoryRes;
import backend.daangnbasedbackend.product.application.dto.ProductCreateReq;
import backend.daangnbasedbackend.product.application.dto.ProductRes;
import backend.daangnbasedbackend.product.application.dto.ProductSummaryRes;
import backend.daangnbasedbackend.product.application.dto.ProductUpdateReq;
import backend.daangnbasedbackend.product.application.provided.ProductCategoryFinder;
import backend.daangnbasedbackend.product.application.provided.ProductFinder;
import backend.daangnbasedbackend.product.application.provided.ProductWriter;
import backend.daangnbasedbackend.product.domain.ProductState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean FavoriteWriter favoriteWriter;
    @MockitoBean ProductWriter productWriter;
    @MockitoBean ProductFinder productFinder;
    @MockitoBean ProductCategoryFinder productCategoryFinder;
    @MockitoBean MemoryMap memoryMap;
    @MockitoBean FavoriteCache favoriteCache;

    private static final Long MEMBER_ID = 1L;
    private static final String BASE_URL = "/v1/products";

    private UsernamePasswordAuthenticationToken auth() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthDetails(MEMBER_ID), null, Collections.emptyList());
    }

    private ProductRes sampleProductRes() {
        return new ProductRes(1L, MEMBER_ID, null, "디지털기기", "아이폰", "좋은 상태",
                BigDecimal.valueOf(500000), "서울", ProductState.ON_SALE,
                0L, 0L, LocalDateTime.now(), null, List.of());
    }

    private ProductSummaryRes sampleSummaryRes() {
        return new ProductSummaryRes(1L, MEMBER_ID, "디지털기기", "아이폰",
                BigDecimal.valueOf(500000), "서울", "thumbnail.jpg",
                ProductState.ON_SALE, 0L, LocalDateTime.now(), null);
    }

    // ==========================================
    // 상품 조회
    // ==========================================

    @Test
    @DisplayName("특정 상품 조회: 200 OK, result=SUCCESS, 상품 정보를 반환한다")
    void getProduct_success() throws Exception {
        // given
        given(productFinder.findById(1L)).willReturn(sampleProductRes());

        // when & then
        mockMvc.perform(get(BASE_URL + "/1")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.title").value("아이폰"));
    }

    // ==========================================
    // 상품 등록/수정/삭제
    // ==========================================

    @Test
    @DisplayName("상품 등록: 200 OK, result=SUCCESS")
    void registerProduct_success() throws Exception {
        // given
        ProductCreateReq req = new ProductCreateReq(1L, "아이폰", "좋은 상태",
                BigDecimal.valueOf(500000), List.of("img.jpg"));
        given(productWriter.create(eq(MEMBER_ID), any(ProductCreateReq.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post(BASE_URL)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("상품 수정: 200 OK, result=SUCCESS")
    void updateProduct_success() throws Exception {
        // given
        ProductUpdateReq req = new ProductUpdateReq(1L, "아이폰 수정", "설명 수정",
                BigDecimal.valueOf(400000), List.of());

        // when & then
        mockMvc.perform(patch(BASE_URL + "/1")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("상품 삭제: 200 OK, result=SUCCESS")
    void deleteProduct_success() throws Exception {
        // when & then
        mockMvc.perform(delete(BASE_URL + "/1")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    // ==========================================
    // 상태 변경
    // ==========================================

    @Test
    @DisplayName("상품 끌어올리기: 200 OK, result=SUCCESS")
    void refreshProduct_success() throws Exception {
        // when & then
        mockMvc.perform(post(BASE_URL + "/1/refresh")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("상품 예약: 200 OK, result=SUCCESS")
    void reserveProduct_success() throws Exception {
        // when & then
        mockMvc.perform(post(BASE_URL + "/1/reserve")
                        .param("buyerId", "2")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("거래 완료: 200 OK, result=SUCCESS")
    void completeTrade_success() throws Exception {
        // when & then
        mockMvc.perform(post(BASE_URL + "/1/complete")
                        .param("buyerId", "2")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("예약 취소: 200 OK, result=SUCCESS")
    void cancelReservation_success() throws Exception {
        // when & then
        mockMvc.perform(post(BASE_URL + "/1/cancel-reservation")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    // ==========================================
    // 관심 & 마이페이지
    // ==========================================

    @Test
    @DisplayName("관심 토글: 200 OK, result=SUCCESS, 토글 후 찜 여부와 좋아요 수 반환")
    void toggleFavorite_success() throws Exception {
        // given
        given(favoriteWriter.toggleFavorite(MEMBER_ID, 1L))
                .willReturn(new backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes(true, 5L));

        // when & then
        mockMvc.perform(post(BASE_URL + "/1/favorite")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    @DisplayName("찜 여부 조회 — 찜한 상품: 200 OK, data=true")
    void isFavorited_true() throws Exception {
        // given
        given(productFinder.isFavorited(MEMBER_ID, 1L)).willReturn(true);

        // when & then
        mockMvc.perform(get(BASE_URL + "/1/favorite")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("찜 여부 조회 — 찜하지 않은 상품: 200 OK, data=false")
    void isFavorited_false() throws Exception {
        // given
        given(productFinder.isFavorited(MEMBER_ID, 1L)).willReturn(false);

        // when & then
        mockMvc.perform(get(BASE_URL + "/1/favorite")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("나의 관심 목록 조회: 200 OK, result=SUCCESS, 페이지 데이터를 반환한다")
    void getMyFavoriteProducts_success() throws Exception {
        // given
        given(productFinder.findFavoriteProducts(eq(MEMBER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleSummaryRes())));

        // when & then
        mockMvc.perform(get(BASE_URL + "/me/favorites")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("나의 판매 내역 조회: 200 OK, result=SUCCESS, 판매 목록을 반환한다")
    void getMySellingProducts_success() throws Exception {
        // given
        given(productFinder.findProductsBySellerId(eq(MEMBER_ID), eq(ProductState.ON_SALE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleSummaryRes())));

        // when & then
        mockMvc.perform(get(BASE_URL + "/me/selling")
                        .param("state", "ON_SALE")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("나의 구매 내역 조회: 200 OK, result=SUCCESS, 구매 목록을 반환한다")
    void getMyBuyingProducts_success() throws Exception {
        // given
        given(productFinder.findProductsByBuyerId(eq(MEMBER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleSummaryRes())));

        // when & then
        mockMvc.perform(get(BASE_URL + "/me/buying")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ==========================================
    // 카테고리
    // ==========================================

    @Test
    @DisplayName("카테고리 목록 조회: 200 OK, result=SUCCESS, 카테고리 배열을 반환한다")
    void getCategories_success() throws Exception {
        // given
        given(productCategoryFinder.findAll())
                .willReturn(List.of(
                        new ProductCategoryRes(1L, "디지털기기"),
                        new ProductCategoryRes(2L, "가구/인테리어")));

        // when & then
        mockMvc.perform(get(BASE_URL + "/categories")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("디지털기기"));
    }

    // ==========================================
    // 인증 실패
    // ==========================================

    @Test
    @DisplayName("미인증 요청: 인증 없이 보호된 엔드포인트에 접근하면 4xx를 반환한다")
    void unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().is4xxClientError());
    }
}
