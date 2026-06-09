package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    Page<Product> findBySellerIdAndStateAndIsDeletedFalse(Long sellerId, ProductState state, Pageable pageable);

    Page<Product> findByBuyerIdAndIsDeletedFalse(Long buyerId, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE p.id IN (SELECT f.productId FROM FavoriteProduct f WHERE f.memberId = :memberId) " +
            "AND p.isDeleted = false")
    Page<Product> findFavoritesByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE products SET like_count = GREATEST(0, like_count + :delta) WHERE id = :productId",
            nativeQuery = true)
    void updateLikeCount(@Param("productId") Long productId, @Param("delta") long delta);
}
