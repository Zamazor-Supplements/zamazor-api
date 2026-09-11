package com.zamazor.market.mail.listener;

import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.mail.event.OrderPlacedEvent;
import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.mail.factory.EmailContextFactory;
import com.zamazor.market.mail.service.EmailService;
import com.zamazor.market.modules.catalog.models.mapper.OrderItemMapper;
import com.zamazor.market.modules.catalog.models.mapper.OrderMapper;
import com.zamazor.market.modules.dashboard.events.AdminEvent;
import com.zamazor.market.modules.dashboard.service.AdminSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailListener {
	private final EmailService emailService;
	private final ApplicationProperties application;
	private final OrderItemMapper orderItemMapper;
	private final AdminSseService adminSseService;
	private final EmailContextFactory emailContextFactory;
	private final OrderMapper orderMapper;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderPlacedNotification(OrderPlacedEvent event) {
		var order = event.order();
		var user = event.order().getUser();
		var eventId = "evt_%s".formatted(UUID.randomUUID());
		Map<String, Object> variables = emailContextFactory.create(order);

		variables.put("paymentUrl", "%s/orders/%s/pay".formatted(application.backendUrl(), order.getId()));
		variables.put("items", order.getItems().stream().map(orderItemMapper::toDto).toList());
		variables.put("totalAmount", "MAD %s".formatted(order.getTotal()));

		emailService.sendHtmlEmail(
				user.getEmail(),
				"Action Required: Complete your order #%s".formatted(order.getId()),
				"checkout-success",
				variables
		);

		adminSseService.publish(new AdminEvent(
				eventId,
				"order.created",
				orderMapper.toDto(order)
		));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderNotifications(OrderStatusChangedEvent event) {
		String userEmail = event.order().getUser().getEmail();
		var order = event.order();
		String orderIdStr = order.getId().toString();
		var eventId = "evt_%s".formatted(UUID.randomUUID());
		Map<String, Object> variables = emailContextFactory.create(order);

		switch (order.getStatus()) {
			case CONFIRMED -> {
				variables.put("items", order.getItems().stream().map(orderItemMapper::toDto).toList());
				variables.put("totalAmount", order.getTotal() + "MAD");
				emailService.sendHtmlEmail(
						userEmail,
						"Payment Confirmed - Order #%s".formatted(orderIdStr),
						"order-success-receipt",
						variables
				);
			}
			case CANCELED -> emailService.sendHtmlEmail(
					userEmail,
					"Your Order #%s has been cancelled".formatted(orderIdStr),
					"order-canceled",
					variables
			);
			case REFUNDED -> emailService.sendHtmlEmail(
					userEmail,
					"Refund Confirmed for Order #%s".formatted(orderIdStr),
					"order-refunded",
					variables
			);
			default -> log.debug("No notification email needed for order status: {}", order.getStatus());
		}

		adminSseService.publish(new AdminEvent(
				eventId,
				"order.updated",
				orderMapper.toDto(order)
		));
	}
}