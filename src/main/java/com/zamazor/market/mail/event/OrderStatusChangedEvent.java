package com.zamazor.market.mail.event;

import com.zamazor.market.modules.catalog.models.dto.OrderItemDto;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderStatusChangedEvent(
		UUID orderId,
		String userEmail,
		OrderStatus status,
		BigDecimal totalAmount,
		List<OrderItemDto> items
) {
}