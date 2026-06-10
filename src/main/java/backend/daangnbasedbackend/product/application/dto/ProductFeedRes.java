package backend.daangnbasedbackend.product.application.dto;

import java.util.List;

public record ProductFeedRes(
    List<ProductSummaryRes> products,
    String nextCursor,
    boolean hasNext
) {}
