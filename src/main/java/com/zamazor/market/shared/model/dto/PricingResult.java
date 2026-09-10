package com.zamazor.market.shared.model.dto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record PricingResult(
		BigDecimal subtotal,
		@Nullable BigDecimal discount,
		@Nullable BigDecimal tax,
		@Nullable BigDecimal shipping,
		BigDecimal total
) {
}
