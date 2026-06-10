package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findBySellerIdAndStateAndIsDeletedFalse(Long sellerId, ProductState state, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByBuyerIdAndIsDeletedFalse(Long buyerId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p " +
            "WHERE p.id IN (SELECT f.productId FROM FavoriteProduct f WHERE f.memberId = :memberId AND f.isDeleted = false) " +
            "AND p.isDeleted = false")
    Page<Product> findFavoritesByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE products SET like_count = GREATEST(0, like_count + :delta) WHERE id = :productId",
            nativeQuery = true)
    void updateLikeCount(@Param("productId") Long productId, @Param("delta") long delta);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.location = :location AND p.isDeleted = false ORDER BY p.refreshAt DESC, p.id DESC")
    List<Product> findFeed(@Param("location") String location, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.location = :location AND p.isDeleted = false AND p.state = :state ORDER BY p.refreshAt DESC, p.id DESC")
    List<Product> findFeedByState(@Param("location") String location, @Param("state") ProductState state, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.location = :location AND p.isDeleted = false AND (p.refreshAt < :cursorRefreshedAt OR (p.refreshAt = :cursorRefreshedAt AND p.id < :cursorId)) ORDER BY p.refreshAt DESC, p.id DESC")
    List<Product> findFeedAfterCursor(@Param("location") String location, @Param("cursorRefreshedAt") LocalDateTime cursorRefreshedAt, @Param("cursorId") Long cursorId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.location = :location AND p.isDeleted = false AND p.state = :state AND (p.refreshAt < :cursorRefreshedAt OR (p.refreshAt = :cursorRefreshedAt AND p.id < :cursorId)) ORDER BY p.refreshAt DESC, p.id DESC")
    List<Product> findFeedByStateAfterCursor(@Param("location") String location, @Param("state") ProductState state, @Param("cursorRefreshedAt") LocalDateTime cursorRefreshedAt, @Param("cursorId") Long cursorId, Pageable pageable);
}
