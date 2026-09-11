package com.zamazor.market.payment.handler;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zamazor.market.modules.catalog.models.dto.ReserveLine;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.exception.WebhookMismatchException;
import com.zamazor.market.payment.util.StripeObjects;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentTerminalHandler implements StripeEventHandler {
	private final OrderRepository orders;
	private final ProductRepository productRepository;

	@Override
	public Set<String> eventTypes() {
		return Set.of("payment_intent.payment_failed", "payment_intent.canceled");
	}

	@Override
	@Transactional
	public UUID handle(Event event) {
		PaymentIntent intent = StripeObjects.deserialize(event, PaymentIntent.class);

		Order order = orders.findByStripePaymentIntentId(intent.getId())
				.orElseThrow(() -> new WebhookMismatchException(
						"No order for payment intent %s".formatted(intent.getId())));

		// Never un-pay an order that already succeeded.
		if (order.getStatus() != OrderStatus.CONFIRMED) {
			order.markPaymentFailedOrCanceled(event.getType());   // flushed by first bulk call below

			var lineItems = order.getItems().stream()
					.map(item -> new ReserveLine(item.getProductId(), item.getQuantity()))
					.toList();

			for (ReserveLine line : lineItems) {
				if (productRepository.releaseStock(line.productId(), line.quantity()) == 0) {
					log.warn("releaseStock no-op for product {} on order {} — nothing reserved",
							line.productId(), order.getId());
				}
			}

			log.info("Order {} -> FAILED via Stripe event {} ({})",
					order.getId(), event.getId(), event.getType());
		} else {
			log.debug("Order {} already PAID — ignoring terminal intent event {}",
					order.getId(), intent.getId());
		}
		return order.getId();
	}
}