package com.zamazor.market.modules.dashboard.models.dto;

public record CategoryAnalyticsDto(
		String id,
		String label,
		long productCount
) {
}
