package com.zamazor.market.modules.catalog.models.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
	PENDING,
	CONFIRMED,
	SHIPPED,
	DELIVERED,
	CANCELED,
	REFUNDED;

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
			PENDING, EnumSet.of(CONFIRMED, CANCELED),
			CONFIRMED, EnumSet.of(SHIPPED, CANCELED, REFUNDED),
			SHIPPED, EnumSet.of(DELIVERED, REFUNDED),
			DELIVERED, EnumSet.of(REFUNDED),
			CANCELED, EnumSet.noneOf(OrderStatus.class),
			REFUNDED, EnumSet.noneOf(OrderStatus.class)
	);

	public boolean canTransitionTo(OrderStatus targetStatus) {
		if (this == targetStatus) {
			return true;
		}
		return ALLOWED.getOrDefault(this, Set.of()).contains(targetStatus);
	}

	public boolean isTerminal() {
		return this == CANCELED || this == REFUNDED;
	}
}