package com.zamazor.market.modules.dashboard.models.dto;

import java.math.BigDecimal;

public record ProductMetrics(
		long totalProducts,
		long totalCategories,
		long lowStockCount,
		BigDecimal averagePrice
) {
}