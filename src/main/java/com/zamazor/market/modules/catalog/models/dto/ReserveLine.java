package com.zamazor.market.modules.catalog.models.dto;

import java.util.UUID;

public record ReserveLine(
		UUID productId,
		int quantity
) {
}