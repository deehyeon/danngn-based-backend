package backend.daangnbasedbackend.product.application.dto;

import backend.daangnbasedbackend.product.domain.ProductCategory;

public record ProductCategoryRes(
        Long categoryId,
        String name
) {
    public static ProductCategoryRes from(ProductCategory category) {
        return new ProductCategoryRes(category.getId(), category.getName());
    }
}
