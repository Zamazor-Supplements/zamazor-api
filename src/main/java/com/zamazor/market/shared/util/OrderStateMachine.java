package com.zamazor.market.shared.util;

import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderStateMachine {
	public static void verify(OrderStatus from, OrderStatus to) {
		if (from == to) return;
		if (!from.canTransitionTo(to)) {
			throw new IllegalOrderTransitionException("transition %s -> %s is not allowed".formatted(from, to));
		}
	}

	public static boolean isTerminal(OrderStatus s) {
		return s.isTerminal();
	}
}