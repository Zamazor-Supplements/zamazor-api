package com.zamazor.market.modules.product.models.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
		@Nullable @Size(min = 2, max = 200) String name,
		@Nullable @Size(max = 200) String description,
		@Nullable @Positive BigDecimal price,
		@Nullable @Positive Integer stockQuantity,
		@Nullable UUID categoryId
) {
}