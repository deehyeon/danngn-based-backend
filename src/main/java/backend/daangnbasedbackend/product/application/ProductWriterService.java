package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ProductCreateReq;
import backend.daangnbasedbackend.product.application.dto.ProductUpdateReq;
import backend.daangnbasedbackend.product.application.provided.ProductWriter;
import backend.daangnbasedbackend.product.application.required.ProductCategoryRepository;
import backend.daangnbasedbackend.product.application.required.ProductRepository;
import backend.daangnbasedbackend.product.domain.Product;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import backend.daangnbasedbackend.product.domain.ProductState;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductWriterService implements ProductWriter {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    public Long create(Long sellerId, ProductCreateReq req) {
        ProductCategory category = findCategoryById(req.categoryId());
        Product product = Product.create(
                sellerId,
                category,
                req.title(),
                req.description(),
                req.price(),
                req.location(),
                req.imageUrls() != null ? req.imageUrls() : List.of()
        );
        return productRepository.save(product).getId();
    }

    @Override
    public void update(Long sellerId, Long productId, ProductUpdateReq req) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);

        if (req == null) {
            product.refresh();
            return;
        }

        ProductCategory category = findCategoryById(req.categoryId());
        product.updateProduct(
                category,
                req.title(),
                req.description(),
                req.price(),
                req.location(),
                req.imageUrls() != null ? req.imageUrls() : List.of()
        );
    }

    @Override
    public void delete(Long sellerId, Long productId) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);
        product.softDelete();
    }

    @Override
    public void refresh(Long sellerId, Long productId) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);
        product.refresh();
    }

    @Override
    public void reserve(Long sellerId, Long productId, Long buyerId) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);
        validateNotSelfTrade(sellerId, buyerId);
        product.changeState(ProductState.RESERVED);
        product.assignBuyer(buyerId);
    }

    @Override
    public void completeTrade(Long sellerId, Long productId, Long buyerId) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);
        validateNotSelfTrade(sellerId, buyerId);
        product.changeState(ProductState.SOLD_OUT);
        product.assignBuyer(buyerId);
    }

    @Override
    public void cancelReservation(Long sellerId, Long productId) {
        Product product = findActiveById(productId);
        validateOwner(product, sellerId);
        product.cancelReservation();
    }

    private ProductCategory findCategoryById(Long categoryId) {
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_CATEGORY_NOT_FOUND));
    }

    private Product findActiveById(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_NOT_FOUND));
    }

    private void validateOwner(Product product, Long sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductException(ProductErrorType.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateNotSelfTrade(Long sellerId, Long buyerId) {
        if (sellerId.equals(buyerId)) {
            throw new ProductException(ProductErrorType.INVALID_TRADE_REQUEST);
        }
    }
}