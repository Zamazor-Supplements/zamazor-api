package com.zamazor.market.shared.util;

import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class OrderStateMachine {

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
			OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELED),
			OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELED, OrderStatus.REFUNDED),
			OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
			OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUNDED),
			OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class),
			OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class)
	);

	public static void verify(OrderStatus from, OrderStatus to) {
		if (from == to) return;
		if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
			throw new IllegalOrderTransitionException("transition %s -> %s is not allowed".formatted(from, to));
		}
	}

	public static boolean isTerminal(OrderStatus s) {
		return s == OrderStatus.CANCELED || s == OrderStatus.REFUNDED;
	}
}