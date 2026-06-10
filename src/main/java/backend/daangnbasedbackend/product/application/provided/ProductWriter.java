package backend.daangnbasedbackend.product.application.provided;

import backend.daangnbasedbackend.product.application.dto.ProductCreateReq;
import backend.daangnbasedbackend.product.application.dto.ProductUpdateReq;

public interface ProductWriter {
    Long create(Long sellerId, ProductCreateReq req);
    void update(Long sellerId, Long productId, ProductUpdateReq req);
    void delete(Long sellerId, Long productId);
    void refresh(Long sellerId, Long productId);
    void reserve(Long sellerId, Long productId, Long buyerId);
    void completeTrade(Long sellerId, Long productId, Long buyerId);
    void cancelReservation(Long sellerId, Long productId);
}