package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.global.annotation.lock.DistributedLock;
import backend.daangnbasedbackend.product.application.dto.ToggleFavoriteRes;
import backend.daangnbasedbackend.product.application.provided.FavoriteWriter;
import backend.daangnbasedbackend.product.application.required.FavoriteCacheRepository;
import backend.daangnbasedbackend.product.application.required.FavoriteProductRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.FavoriteProduct;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteWriterService implements FavoriteWriter {
    private final ProductRepository productRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final FavoriteCacheRepository favoriteCacheRepository;

    @DistributedLock(key = "'favorite:member:' + #memberId + ':product:' + #productId")
    public ToggleFavoriteRes toggleFavorite(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_NOT_FOUND));

        long baseLikeCount = product.getLikeCount();
        long existingDelta = favoriteCacheRepository.getPendingLikeChange(productId);

        boolean isFavorited;
        long deltaChange;

        if (favoriteProductRepository.existsByMemberIdAndProductId(memberId, productId)) {
            favoriteProductRepository.deleteByMemberIdAndProductId(memberId, productId);
            deltaChange = -1L;
            isFavorited = false;
        } else {
            favoriteProductRepository.save(FavoriteProduct.create(memberId, productId));
            deltaChange = 1L;
            isFavorited = true;
        }

        favoriteCacheRepository.recordLikeChange(productId, deltaChange);
        long currentLikeCount = Math.max(0, baseLikeCount + existingDelta + deltaChange);

        return new ToggleFavoriteRes(isFavorited, currentLikeCount);
    }
}
