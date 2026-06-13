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

import java.util.Optional;

// @Transactional을 클래스/메서드 레벨에 붙이지 말 것.
// toggleFavorite은 @DistributedLock → AopForTransaction(REQUIRES_NEW) 구조로 트랜잭션을 관리한다.
// 호출자에 부모 트랜잭션이 존재하면 REQUIRES_NEW가 추가 커넥션을 요구해 커넥션 풀 고갈 위험이 생기고,
// REQUIRED로 부모 트랜잭션에 합류할 경우 락 해제 전에 커밋이 보장되지 않아 레이스 컨디션이 발생한다.
@Service
@RequiredArgsConstructor
public class FavoriteWriterService implements FavoriteWriter {
    private final ProductRepository productRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final FavoriteCacheRepository favoriteCacheRepository;

    @DistributedLock(key = "'favorite:member:' + #memberId + ':product:' + #productId")
    public ToggleFavoriteRes toggleFavorite(Long memberId, Long productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_NOT_FOUND));

        long baseLikeCount = product.getLikeCount();
        long existingDelta = favoriteCacheRepository.getPendingLikeChange(productId);

        boolean isFavorited;
        long deltaChange;

        Optional<FavoriteProduct> existing = favoriteProductRepository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isPresent()) {
            FavoriteProduct fav = existing.get();
            if (!fav.getIsDeleted()) {
                fav.softDelete();
                deltaChange = -1L;
                isFavorited = false;
            } else {
                fav.restore();
                deltaChange = 1L;
                isFavorited = true;
            }
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
