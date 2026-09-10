package com.zamazor.market.modules.dashboard.models.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverviewDto(
		BigDecimal totalSales,
		BigDecimal averageOrderValue,
		long totalOrders,
		long pendingOrders,
		long completedOrders,
		long canceledOrders,
		long inFlightOrders,
		List<RecentOrderDto> recentOrders,
		List<LowStockProductDto> lowStockProducts,
		List<CategorySummaryDto> categorySummary,
		List<TopProductDto> topProducts
) {
}