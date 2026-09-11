package com.zamazor.market.modules.dashboard.events;

public record AdminEvent(
		String id,
		String type,
		Object data
) {
}
