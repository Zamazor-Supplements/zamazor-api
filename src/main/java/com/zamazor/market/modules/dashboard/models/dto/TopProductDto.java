package com.zamazor.market.modules.dashboard.models.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductDto(
		UUID id,
		String name,
		int quantity,
		BigDecimal revenue,
		String category
) {
}