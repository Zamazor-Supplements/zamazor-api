package com.zamazor.market.mail.event;

import com.zamazor.market.modules.catalog.models.entity.Order;

public record OrderStatusChangedEvent(Order order) {
}