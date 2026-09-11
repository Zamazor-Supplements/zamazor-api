package com.zamazor.market.modules.catalog.service;

import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.catalog.exception.*;
import com.zamazor.market.modules.catalog.models.dto.*;
import com.zamazor.market.modules.catalog.models.mapper.OrderItemMapper;
import com.zamazor.market.payment.exception.PaymentGatewayException;
import com.zamazor.market.payment.service.PaymentService;
import com.zamazor.market.shared.api.PageResponse;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.models.mapper.OrderMapper;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.catalog.specification.OrderSpecifications;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final OrderFulfillmentService orderFulfillmentService;
	private final ApplicationEventPublisher publisher;
	private final PaymentService paymentService;
	private final Clock clock;

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
		Instant now = Instant.now(clock);

		if (order.getStatus() == newStatus) return orderMapper.toDto(order);

		switch (newStatus) {
			case CANCELED -> orderFulfillmentService.cancelOrder(order, now, PaymentStatus.CANCELED);
			case REFUNDED -> orderFulfillmentService.refundOrder(order, now);
			default -> order.transitionTo(newStatus);
		}

		publishStatusChangedEvent(order);
		return orderMapper.toDto(order);
	}

	@Transactional
	public OrderDto cancelOrder(UUID orderId, PaymentStatus targetPaymentStatus) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		if (order.getStatus() == OrderStatus.CANCELED) {
			return orderMapper.toDto(order);
		}

		Instant now = Instant.now(clock);
		orderFulfillmentService.cancelOrder(order, now, targetPaymentStatus);

		publishStatusChangedEvent(order);
		return orderMapper.toDto(order);
	}

	@Transactional
	public OrderDto cancelOrder(UUID orderId) {
		return cancelOrder(orderId, PaymentStatus.CANCELED);
	}

	@Transactional
	public OrderDto verifyOrderPayment(UUID orderId) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		if (paymentService.isSessionUnPaid(order.getStripeCheckoutSessionId())) {
			throw new PaymentGatewayException("Payment verification failed. Transaction is incomplete or declined.");
		}

		return orderMapper.toDto(order);
	}

	private void publishStatusChangedEvent(Order order) {
		var items = order.getItems().stream().map(orderItemMapper::toDto).toList();
		publisher.publishEvent(new OrderStatusChangedEvent(
				order.getId(),
				order.getUser().getEmail(),
				order.getStatus(),
				order.getTotal(),
				items
		));
	}
}