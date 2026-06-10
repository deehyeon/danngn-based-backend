package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {
    boolean existsByMemberIdAndProductIdAndIsDeletedFalse(Long memberId, Long productId);
    Optional<FavoriteProduct> findByMemberIdAndProductId(Long memberId, Long productId);
}
