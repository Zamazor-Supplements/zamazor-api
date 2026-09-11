package com.zamazor.market.modules.catalog.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final PaymentService paymentService;
	private final OrderPolicyProperties policy;

	@Transactional
	public void cancelOrder(Order order, Instant now, PaymentStatus targetPaymentStatus) {
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
	public void refundOrder(Order order, Instant now) {
		if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CONFIRMED) {
			throw new IllegalOrderTransitionException("Only paid or delivered orders can be refunded.");
		}

		if (order.getStripePaymentIntentId() != null) {
			try {
				paymentService.refundPayment(order.getStripePaymentIntentId(), order.getTotal());
			} catch (Exception e) {
				log.error("Failed to process gateway refund for order {}: {}", order.getId(), e.getMessage());
				throw new PaymentGatewayException("Refund gateway communication failed: " + e.getMessage());
			}
		}

		List<StockRestoreDto> itemsToRestore = order.refund(now, policy);
		if (!itemsToRestore.isEmpty()) {
			restoreInventoryStock(itemsToRestore);
		}
	}

	private void restoreInventoryStock(List<StockRestoreDto> itemsToRestore) {
		for (StockRestoreDto item : itemsToRestore) {
			if (productRepository.restoreAvailability(item.productId(), item.quantity()) == 0) {
				log.warn("restoreAvailability no-op for product {} qty {} — already restored?",
						item.productId(), item.quantity());
			}
		}
	}
}