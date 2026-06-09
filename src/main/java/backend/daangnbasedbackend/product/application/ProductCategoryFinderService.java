package backend.daangnbasedbackend.product.application;

import backend.daangnbasedbackend.product.application.dto.ProductCategoryRes;
import backend.daangnbasedbackend.product.application.provided.ProductCategoryFinder;
import backend.daangnbasedbackend.product.application.required.ProductCategoryRepository;
import backend.daangnbasedbackend.product.domain.ProductCategory;
import backend.daangnbasedbackend.product.exception.ProductErrorType;
import backend.daangnbasedbackend.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductCategoryFinderService implements ProductCategoryFinder {
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    public ProductCategoryRes findById(Long categoryId) {
        return productCategoryRepository.findById(categoryId)
                .map(ProductCategoryRes::from)
                .orElseThrow(() -> new ProductException(ProductErrorType.PRODUCT_CATEGORY_NOT_FOUND));
    }

    @Override
    public List<ProductCategoryRes> findAll() {
        return productCategoryRepository.findAll().stream()
                .map(ProductCategoryRes::from)
                .toList();
    }
}
