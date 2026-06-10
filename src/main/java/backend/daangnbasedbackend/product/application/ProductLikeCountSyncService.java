package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.required.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ProductLikeCountSyncService {
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLikeCount(Long productId, long delta) {
        productRepository.updateLikeCount(productId, delta);
    }
}
