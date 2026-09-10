package com.zamazor.market.payment.handler;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.zamazor.market.modules.catalog.models.dto.StockRestoreDto;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.exception.WebhookMismatchException;
import com.zamazor.market.payment.util.StripeObjects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Charge;
import com.stripe.model.Event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeRefundedEventHandler implements StripeEventHandler {
	private final OrderRepository orders;
	private final Clock clock;
	private final ProductRepository productRepository;

	@Override
	public Set<String> eventTypes() {
		return Set.of("charge.refunded");
	}

	@Override
	@Transactional
	public UUID handle(Event event) {
		Charge charge = StripeObjects.deserialize(event, Charge.class);

		String paymentIntentId = charge.getPaymentIntent();
		if (paymentIntentId == null) {
			log.warn("charge.refunded {} has no payment_intent — cannot resolve order", charge.getId());
			return null;
		}

		Order order = orders.findByStripePaymentIntentIdForUpdate(paymentIntentId)
				.orElseThrow(() ->
						new WebhookMismatchException(
								"No order for payment intent %s (charge %s)".formatted(paymentIntentId, charge.getId()))
				);

		// Monotonic by contract: recordRefundApplied ignores values <= current.
		List<StockRestoreDto> itemsToRestore = order.recordRefundApplied(
				BigDecimal.valueOf(charge.getAmountRefunded()).movePointLeft(2),
				Instant.now(clock)
		);

		if (!itemsToRestore.isEmpty()) {
			for (StockRestoreDto item : itemsToRestore) {
				productRepository.restoreAvailability(item.productId(), item.quantity());
			}
			log.info("Restored stock for {} items due to charge.refunded on order {}", itemsToRestore.size(), order.getId());
		}

		log.info("Charge {} refunded {} — order {} refunded amount reconciled",
				charge.getId(), charge.getAmountRefunded(), order.getId());
		return order.getId();
	}
}