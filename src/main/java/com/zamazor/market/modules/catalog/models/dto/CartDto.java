package com.zamazor.market.modules.catalog.models.dto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartDto(
		UUID id,
		List<CartItemDto> items,
		BigDecimal subtotal,
		@Nullable BigDecimal tax,
		@Nullable BigDecimal shipping,
		@Nullable BigDecimal discount,
		BigDecimal total
) {
}