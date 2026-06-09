package backend.daangnbasedbackend.product.application.provided;

import backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes;

public interface FavoriteWriter {
    ToggleFavoriteRes toggleFavorite(Long memberId, Long productId);
}