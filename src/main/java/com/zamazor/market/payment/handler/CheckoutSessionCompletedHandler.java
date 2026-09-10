package com.zamazor.market.payment.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.stripe.model.checkout.Session;
import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.modules.catalog.models.dto.ReserveLine;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.mapper.OrderItemMapper;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.exception.WebhookMismatchException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSessionCompletedHandler implements StripeEventHandler {
	private final OrderRepository orderRepository;
	private final Clock clock;
	private final ApplicationEventPublisher publisher;
	private final OrderItemMapper orderItemMapper;
	private final ProductRepository productRepository;

	@Override
	public Set<String> eventTypes() {
		return Set.of("checkout.session.completed");
	}

	@Override
	@Transactional
	public UUID handle(Event event) {

		Session session = (Session) event.getDataObjectDeserializer()
				.getObject()
				.orElseThrow(() -> new IllegalStateException(
						"checkout.session.completed event %s has no deserializable Session".formatted(event.getId())));

		// 1. Find the order bound to this session id (server side, not from the payload)
		Order order = orderRepository.findByStripeCheckoutSessionId(session.getId())
				.orElseThrow(() -> new WebhookMismatchException("No order bound to Stripe session %s".formatted(session.getId())));

		// 2. Verify client_reference_id matches our UUID
		UUID expectedOrderId = order.getId();
		UUID claimedOrderId = parseUuid(session.getClientReferenceId());
		if (!expectedOrderId.equals(claimedOrderId)) {
			throw new WebhookMismatchException("client_reference_id mismatch — order=%s, stripe=%s"
					.formatted(expectedOrderId, claimedOrderId)
			);
		}

		// 3. Verify amount matches the order's server-recomputed total, in units
		long claimedAmount = Optional.ofNullable(session.getAmountTotal()).orElse(0L);
		long orderTotal = order.getTotal().movePointRight(2).longValue();
		if (!Objects.equals(orderTotal, claimedAmount)) {
			throw new WebhookMismatchException("amount_total mismatch — order total=%d, stripeAmount=%d"
					.formatted(orderTotal, claimedAmount)
			);
		}

		// 4. Verify currency matches
		if (!Objects.equals("mad",
				Optional.ofNullable(session.getCurrency()).orElse(""))) {
			throw new WebhookMismatchException("currency mismatch — order='%s', stripe=%s"
					.formatted("mad", session.getCurrency())
			);
		}

		// 5. Stripe itself says it's not paid — log and skip; the next event may disagree
		if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
			log.warn("Stripe session {} paymentStatus='{}' — not marking order paid",
					session.getId(), session.getPaymentStatus());
			return order.getId();
		}

		Instant now = Instant.now(clock);
		order.markPaid(session.getPaymentIntent(), now);

		// Collect pairs + DTOs BEFORE the bulk updates (persistence context is cleared by @Modifying).
		var lineItems = order.getItems().stream()
				.map(item -> new ReserveLine(item.getProductId(), item.getQuantity()))
				.toList();

		for (ReserveLine line : lineItems) {
			if (productRepository.confirmReservation(line.productId(), line.quantity()) == 0) {
				log.warn("confirmReservation no-op for product {} on order {} — nothing was reserved",
						line.productId(), order.getId());
			}
		}

		var user = order.getUser();
		var items = order.getItems().stream().map(orderItemMapper::toDto).toList();

		publisher.publishEvent(new OrderStatusChangedEvent(order.getId(), user.getEmail(), order.getStatus(), order.getTotal(), items));
		log.info("Order {} marked PAID via Stripe event {}", order.getId(), event.getId());
		return order.getId();
	}

	private static UUID parseUuid(String s) {
		if (s == null) throw new WebhookMismatchException("%s is null"
				.formatted("client_reference_id")
		);
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			throw new WebhookMismatchException("%s is not a UUID: %s"
					.formatted("client_reference_id", s)
			);
		}
	}
}