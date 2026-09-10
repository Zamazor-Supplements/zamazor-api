package com.zamazor.market.mail.event;

import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.user.models.entity.User;

public record OrderPlacedEvent(Order order, User user, String paymentUrl) {
}