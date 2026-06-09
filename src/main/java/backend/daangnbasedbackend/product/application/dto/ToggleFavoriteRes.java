package backend.daangnbasedbackend.product.application.dto;

public record ToggleFavoriteRes(
        boolean favorited,  // 토글 후 현재 찜 상태
        long likeCount      // 토글 후 현재 좋아요 수 (DB 기준값 + 아직 반영 안 된 Redis delta)
) {}
