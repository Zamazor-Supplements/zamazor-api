package com.zamazor.market.modules.product.models.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkProductRequest(
		@NotEmpty
		@Size(max = 100, message = "Cannot fetch more than 100 products at once")
		List<UUID> ids
) {
}