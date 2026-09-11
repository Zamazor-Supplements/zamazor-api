package com.zamazor.market.modules.catalog.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.catalog.models.dto.StockRestoreDto;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.config.OrderPolicyProperties;
import com.zamazor.market.payment.exception.PaymentGatewayException;
import com.zamazor.market.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final PaymentService paymentService;
	private final OrderPolicyProperties policy;
	private final Clock clock;
	private final ApplicationEventPublisher publisher;

	@Transactional
	public void cancelOrder(Order order, PaymentStatus targetPaymentStatus) {
		var now = Instant.now(clock);

		if (isStripeSessionPaidAndRecovered(order, now)) {
			return;
		}
		executeCancellation(order, targetPaymentStatus, now);
	}

	private void executeCancellation(Order order, PaymentStatus targetPaymentStatus, Instant now) {
		if (orderRepository.transitionStatus(order.getId(), order.getStatus(), OrderStatus.CANCELED) == 0) {
			if (order.getStatus() == OrderStatus.CANCELED) {
				log.warn("Order {} already canceled — no-op", order.getId());
				return;
			}
			throw new IllegalOrderTransitionException("Cannot cancel order %s in status %s"
					.formatted(order.getId(), order.getStatus()));
		}

		order.setPaymentStatus(targetPaymentStatus);
		paymentService.expireCheckoutSession(order.getStripeCheckoutSessionId());

		List<StockRestoreDto> itemsToRestore = order.cancel(now, policy);
		if (!itemsToRestore.isEmpty()) {
			restoreInventoryStock(itemsToRestore);
		}
	}

	@Transactional
	public void refundOrder(Order order) {
		validateOrderState(order);
		processPaymentRefund(order);
		processRefundInventoryRestoration(order);
	}

	private void validateOrderState(Order order) {
		if (!EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED).contains(order.getStatus())) {
			throw new IllegalOrderTransitionException("Only paid or delivered orders can be refunded.");
		}
	}

	private void processPaymentRefund(Order order) {
		if (order.getStripePaymentIntentId() == null) {
			return;
		}

		try {
			paymentService.refundPayment(order.getStripePaymentIntentId(), order.getTotal());
		} catch (Exception e) {
			log.error("Failed to process gateway refund for order {}: {}", order.getId(), e.getMessage());
			throw new PaymentGatewayException("Refund gateway communication failed: " + e.getMessage());
		}
	}

	private void processRefundInventoryRestoration(Order order) {
		var now = Instant.now(clock);
		List<StockRestoreDto> itemsToRestore = order.refund(now, policy);
		if (!itemsToRestore.isEmpty()) {
			restoreInventoryStock(itemsToRestore);
		}
	}

	private boolean isStripeSessionPaidAndRecovered(Order order, Instant now) {
		String sessionId = order.getStripeCheckoutSessionId();
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}

		try {
			Session session = Session.retrieve(sessionId);

			boolean isComplete = "complete".equals(session.getStatus());
			boolean isPaid = "paid".equals(session.getPaymentStatus()) || "no_payment_required".equals(session.getPaymentStatus());

			if (isComplete && isPaid) {
				log.warn("Attempted to cancel order {}, but Stripe session {} is actually PAID. Recovering order instead.",
						order.getId(), sessionId);
				order.markPaid(session.getPaymentIntent(), now);
				publisher.publishEvent(new OrderStatusChangedEvent(order));
				return true;
			}
		} catch (StripeException e) {
			log.error("Failed to verify Stripe session status during cancellation check for order {}", order.getId(), e);
			throw new IllegalStateException("Failed to verify Stripe session status; aborting cancellation for safety.", e);
		}

		return false;
	}

	private void restoreInventoryStock(List<StockRestoreDto> itemsToRestore) {
		itemsToRestore.forEach(item -> {
			int updatedRows = productRepository.restoreAvailability(item.productId(), item.quantity());
			if (updatedRows == 0) {
				log.warn("restoreAvailability no-op for product {} qty {} — already restored?",
						item.productId(), item.quantity());
			}
		});
	}
}