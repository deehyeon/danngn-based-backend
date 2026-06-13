package backend.daangnbasedbackend.product.application.required;

import backend.daangnbasedbackend.product.domain.ProductState;

import java.util.List;

public interface ProductSearchPort {
    List<Long> searchProductIds(String keyword, String location, ProductState state, String cursor, int fetchSize);
}
