package com.zamazor.market.mail.event;

import com.zamazor.market.modules.catalog.models.entity.Order;

public record OrderPlacedEvent(Order order, String paymentUrl) {
}