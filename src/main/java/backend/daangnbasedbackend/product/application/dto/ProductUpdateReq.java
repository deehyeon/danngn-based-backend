package backend.daangnbasedbackend.product.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateReq(
        @NotNull Long categoryId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull @Positive BigDecimal price,
        List<String> imageUrls
) {}
