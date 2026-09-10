package com.zamazor.market.modules.dashboard.models.dto;

import java.util.UUID;

public record LowStockProductDto(
		UUID id,
		String name,
		Integer stockQuantity,
		String category
) {
}