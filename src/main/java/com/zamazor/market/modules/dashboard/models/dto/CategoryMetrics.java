package com.zamazor.market.modules.dashboard.models.dto;

public record CategoryMetrics(
		String id,
		String label,
		long productCount
) {
}
