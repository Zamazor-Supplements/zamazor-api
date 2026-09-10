package com.zamazor.market.modules.product.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
		@NotBlank @Size(min = 2, max = 200) String name,
		@Nullable @Size(max = 200) String description,
		@NotNull @Positive BigDecimal price,
		@NotNull @Positive Integer stockQuantity,
		@NotNull UUID categoryId
) {
}
