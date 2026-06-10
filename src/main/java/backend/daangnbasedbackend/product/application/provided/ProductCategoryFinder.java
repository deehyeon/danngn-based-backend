package backend.daangnbasedbackend.product.application.provided;

import backend.daangnbasedbackend.product.application.dto.ProductCategoryRes;

import java.util.List;

public interface ProductCategoryFinder {
    ProductCategoryRes findById(Long categoryId);
    List<ProductCategoryRes> findAll();
}
