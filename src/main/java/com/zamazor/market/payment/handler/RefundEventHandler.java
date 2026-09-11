package com.zamazor.market.payment.handler;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.zamazor.market.modules.billing.models.entity.OrderRefund;
import com.zamazor.market.modules.billing.models.entity.RefundStatus;
import com.zamazor.market.modules.billing.repository.OrderRefundRepository;
import com.zamazor.market.modules.catalog.models.dto.StockRestoreDto;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.exception.WebhookMismatchException;
import com.zamazor.market.payment.util.StripeObjects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;
import com.stripe.model.Refund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEventHandler implements StripeEventHandler {
	private final OrderRepository orders;
	private final OrderRefundRepository refunds;
	private final Clock clock;
	private final ProductRepository productRepository;

	@Override
	public Set<String> eventTypes() {
		// charge.refund.updated is legacy; Stripe recommends refund.updated
		// for all refund types. Its data.object is a Refund, so one handler fits.
		return Set.of("refund.created", "refund.updated", "refund.failed", "charge.refund.updated");
	}

	@Override
	@Transactional
	public UUID handle(Event event) {
		Refund refund = StripeObjects.deserialize(event, Refund.class);

		Order order = resolveOrderForUpdate(refund)
				.orElseThrow(() -> new WebhookMismatchException("No order found for refund %s".formatted(refund.getId())));

		OrderRefund record = refunds.findByStripeRefundId(refund.getId())
				.orElseGet(() -> refunds.save(OrderRefund.createdFrom(refund, order)));

		switch (refund.getStatus()) {
			case "succeeded" -> {
				if (RefundStatus.SUCCEEDED.equals(record.getStatus())) {
					// Replay/no-op: already applied. The order's refunded amount is
					// monotonic, so recomputing would change nothing.
					log.debug("Refund {} already SUCCEEDED — no-op", refund.getId());
					return order.getId();
				}
				record.markSucceeded();
				long succeeded = refunds.sumTotalAmountByStatus(order.getId(), RefundStatus.SUCCEEDED);
				List<StockRestoreDto> itemsToRestore = order.recordRefundApplied(BigDecimal.valueOf(succeeded).movePointLeft(2), Instant.now(clock));
				if (!itemsToRestore.isEmpty()) {
					for (StockRestoreDto item : itemsToRestore) {
						productRepository.restoreAvailability(item.productId(), item.quantity());
					}
					log.debug("Restored stock for {} items due to full refund on order {}", itemsToRestore.size(), order.getId());
				}

				log.info("Refund {} succeeded — order {} refunded {} minor units",
						refund.getId(), order.getId(), succeeded);
			}
			case "failed", "canceled" -> {
				record.markFailed(refund.getFailureReason());
				log.warn("Refund {} for order {} {}: {}",
						refund.getId(), order.getId(), refund.getStatus(), refund.getFailureReason());
				// Intentionally no order mutation: a failed/canceled refund must
				// never un-refund an order that already succeeded.
			}
			case "pending", "requires_action" -> {
				record.markPending();
				log.info("Refund {} for order {} is {}",
						refund.getId(), order.getId(), refund.getStatus());
			}
			default -> log.warn("Unknown refund status '{}' for {}", refund.getStatus(), refund.getId());
		}

		return order.getId();
	}

	private Optional<Order> resolveOrderForUpdate(Refund refund) {
		String paymentIntentId = refund.getPaymentIntent();
		if (paymentIntentId != null) {
			return orders.findByStripePaymentIntentIdForUpdate(paymentIntentId);
		}
		// Refund without a PaymentIntent (e.g. dashboard-initiated) — resolving via
		// charge id requires persisting stripe_charge_id on Order (see schema note).
		return Optional.empty();
	}
}
