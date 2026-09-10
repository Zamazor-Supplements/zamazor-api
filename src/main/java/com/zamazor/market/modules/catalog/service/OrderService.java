package com.zamazor.market.modules.catalog.service;

import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.billing.service.OrderPaymentService;
import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.catalog.exception.*;
import com.zamazor.market.modules.catalog.models.dto.*;
import com.zamazor.market.modules.catalog.models.mapper.OrderItemMapper;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.payment.config.OrderPolicyProperties;
import com.zamazor.market.payment.exception.PaymentGatewayException;
import com.zamazor.market.payment.service.PaymentService;
import com.zamazor.market.shared.api.PageResponse;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.models.mapper.OrderMapper;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.catalog.specification.OrderSpecifications;
import com.zamazor.market.modules.user.models.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final ProductRepository productRepository;
	private final Clock clock;
	private final ApplicationEventPublisher publisher;
	private final PaymentService paymentService;
	private final OrderPolicyProperties policy;
	private final OrderItemMapper orderItemMapper;
	private final OrderPaymentService orderPaymentService;

	public PageResponse<OrderDto> getAll(String userFullName, OrderStatus status, Pageable pageable) {
		Specification<Order> spec = OrderSpecifications.createSpec(userFullName, status);

		Page<Order> orderPage = orderRepository.findAll(spec, pageable);
		return new PageResponse<>(orderPage.map(orderMapper::toDto));
	}

	public PageResponse<OrderDto> getByUserId(UUID userId, Pageable pageable) {
		Page<OrderDto> orderPage = orderRepository.findByUserId(userId, pageable).map(orderMapper::toDto);
		return new PageResponse<>(orderPage);
	}

	public OrderDto getById(UUID id) {
		var order = orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));
		return orderMapper.toDto(order);
	}

	@Transactional
	public OrderDto changeStatus(UUID orderId, OrderStatus newStatus) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
		var user = order.getUser();
		Instant now = Instant.now(clock);

		if (order.getStatus() == newStatus) return orderMapper.toDto(order);

		switch (newStatus) {
			case CANCELED -> handleCancellation(order, now);
			case REFUNDED -> handleRefund(order, now);
			default -> order.transitionTo(newStatus);
		}

		var savedOrder = orderRepository.save(order);
		var items = savedOrder.getItems().stream().map(orderItemMapper::toDto).toList();
		publisher.publishEvent(new OrderStatusChangedEvent(
				orderId,
				user.getEmail(),
				savedOrder.getStatus(),
				savedOrder.getTotal(),
				items
		));

		return orderMapper.toDto(savedOrder);
	}

	@Transactional
	public OrderDto cancelOrder(UUID orderId, PaymentStatus targetPaymentStatus) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
		String userEmail = order.getUser().getEmail();

		if (order.getStatus() == OrderStatus.CANCELED) return orderMapper.toDto(order);

		Instant now = Instant.now(clock);
		handleCancellation(order, now, targetPaymentStatus);

		var savedOrder = orderRepository.save(order);
		var items = savedOrder.getItems().stream().map(orderItemMapper::toDto).toList();
		publisher.publishEvent(new OrderStatusChangedEvent(
				orderId,
				userEmail,
				savedOrder.getStatus(),
				savedOrder.getTotal(),
				items
		));

		return orderMapper.toDto(savedOrder);
	}

	@Transactional
	public OrderDto cancelOrder(UUID orderId) {
		return cancelOrder(orderId, PaymentStatus.CANCELED);
	}

	@Transactional
	public OrderDto verifyOrderPayment(UUID orderId, String sessionId, User user) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		if (!order.getUser().getId().equals(user.getId()))
			throw new UnauthorizedOrderException("You do not have permission for this order");
		if (order.getStatus() == OrderStatus.CONFIRMED)
			return orderMapper.toDto(order);
		if (paymentService.isSessionUnPaid(sessionId)) {
			throw new PaymentGatewayException("Payment verification failed. Transaction is incomplete or declined.");
		}

		return orderPaymentService.confirmPayment(orderId);   // guarded transition + confirm reservations, idempotent
	}

	private void handleCancellation(Order order, Instant now, PaymentStatus targetPaymentStatus) {
		// Guarded PENDING -> CANCELED: the sweeper, user-cancel and webhook cancel race here.
		if (orderRepository.transitionStatus(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELED) == 0) {
			if (order.getStatus() == OrderStatus.CANCELED) {
				log.warn("Order {} already canceled — no-op", order.getId());
				return;
			}
			throw new IllegalOrderTransitionException("Cannot cancel order %s in status %s".
					formatted(order.getId(), order.getStatus())
			);
		}

		order.setPaymentStatus(targetPaymentStatus);

		List<StockRestoreDto> itemsToRestore = order.cancel(now, policy);
		if (!itemsToRestore.isEmpty()) {
			restoreInventoryStock(itemsToRestore);
		}
	}

	private void handleCancellation(Order order, Instant now) {
		handleCancellation(order, now, PaymentStatus.CANCELED);
	}

	private void handleRefund(Order order, Instant now) {
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