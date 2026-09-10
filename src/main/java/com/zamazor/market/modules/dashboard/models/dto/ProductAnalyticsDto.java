package com.zamazor.market.modules.dashboard.models.dto;

import java.math.BigDecimal;

public record ProductAnalyticsDto(
		long totalProducts,
		long totalCategories,
		long lowStockCount,
		BigDecimal averagePrice
) {
}