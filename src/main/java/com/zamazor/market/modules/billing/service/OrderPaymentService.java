package com.zamazor.market.modules.billing.service;

import com.stripe.model.checkout.Session;
import com.zamazor.market.mail.event.OrderPlacedEvent;
import com.zamazor.market.mail.event.OrderStatusChangedEvent;
import com.zamazor.market.modules.billing.models.dto.CheckoutRequest;
import com.zamazor.market.modules.billing.models.dto.PaymentSessionResponse;
import com.zamazor.market.modules.billing.models.mapper.PaymentMapper;
import com.zamazor.market.modules.catalog.exception.*;
import com.zamazor.market.modules.catalog.models.dto.AddressRequest;
import com.zamazor.market.modules.catalog.models.dto.OrderDto;
import com.zamazor.market.modules.catalog.models.dto.ReserveLine;
import com.zamazor.market.modules.catalog.models.entity.AddressComponent;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderItem;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.models.mapper.OrderItemMapper;
import com.zamazor.market.modules.catalog.models.mapper.OrderMapper;
import com.zamazor.market.modules.catalog.repository.CartRepository;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.catalog.service.AddressService;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.payment.service.PaymentService;
import com.zamazor.market.shared.model.dto.PricingResult;
import com.zamazor.market.shared.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {
	private final PaymentMapper paymentMapper;
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final ApplicationEventPublisher publisher;
	private final PaymentService paymentService;
	private final CartRepository cartRepository;
	private final AddressService addressService;
	private final PricingService pricingService;


	@Transactional
	public OrderDto checkout(User user, CheckoutRequest request) {
		var cart = cartRepository.findByUserId(user.getId())
				.orElseThrow(CartNotFoundException::new);

		if (cart.getItems().isEmpty()) throw new EmptyCartException();

		PricingResult pricing = pricingService.calculate(cart, user);

		List<ReserveLine> lineItems = cart.getItems().stream()
				.map(item -> new ReserveLine(item.getProduct().getId(), item.getQuantity()))
				.toList();

		// The actual reservation — atomic guarded UPDATE per line. A 0-row result
		// means oversold; throwing here rolls back earlier lines' reservations too.
		for (ReserveLine line : lineItems) {
			if (productRepository.reserveStock(line.productId(), line.quantity()) == 0) {
				throw new OutOfStockException(productRepository.getReferenceById(line.productId()), line.quantity());
			}
		}

		var order = Order.createFromCart(cart, pricing);
		order.setPaymentAttemptCount(order.getPaymentAttemptCount() + 1);

		var snapshot = new AddressComponent(
				request.country(),
				request.city(),
				request.street(),
				request.phone()
		);
		order.setShippingAddressSnapshot(snapshot);

		if (request.isDefault()) {
			var addressRequest = new AddressRequest(request.country(), request.city(), request.street(), request.phone());
			addressService.createOrUpdate(user, addressRequest);
		}

		var savedOrder = orderRepository.saveAndFlush(order);
		cart.clear();
		cartRepository.saveAndFlush(cart);

		// Stripe session + notification event run AFTER commit: an external failure must
		// not roll back reserved stock. If this callback never completes, the TTL sweeper
		// releases the reservation — self-healing by design.
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					Session session = paymentService.createCheckoutSession(savedOrder);
					publisher.publishEvent(new OrderPlacedEvent(savedOrder, user, session.getUrl()));
				} catch (Exception e) {
					log.error("Post-commit checkout finalization failed for order {}", savedOrder.getId(), e);
				}
			}
		});

		return orderMapper.toDto(savedOrder);
	}

	@Transactional
	public OrderDto confirmPayment(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
		if (order.getStatus() == OrderStatus.CONFIRMED) {
			log.info("Duplicate confirm for order {} — idempotent no-op", orderId);
			return orderMapper.toDto(order);
		}

		// Guarded transition — the webhook, the frontend redirect and a retry race here.
		if (orderRepository.transitionStatus(orderId, order.getStatus(), OrderStatus.CONFIRMED) == 0) {
			throw new IllegalOrderTransitionException("Cannot confirm order '%s' in status %s"
					.formatted(orderId, order.getStatus())
			);
		}

		// Order reloaded fresh after the bulk transition (context was cleared).
//		var order = orderRepository.findById(orderId).orElseThrow();
		var user = order.getUser();
		var lineItems = order.getItems().stream()
				.map(item -> new ReserveLine(item.getProductId(), item.getQuantity()))
				.toList();
		var items = lineItems.stream().map(li -> orderItemMapper.toDto(findItem(order, li))).toList();

		for (ReserveLine line : lineItems) {
			if (productRepository.confirmReservation(line.productId(), line.quantity()) == 0) {
				log.warn("confirmReservation no-op for product {} on order {} — nothing was reserved",
						line.productId(), orderId);
			}
		}

		publisher.publishEvent(new OrderStatusChangedEvent(
				orderId, user.getEmail(), OrderStatus.CONFIRMED, order.getTotal(), items));
		return orderMapper.toDto(order);
	}

	@Transactional
	public PaymentSessionResponse regeneratePaymentLink(UUID orderId) {
		var order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		//  Order is already processed or completed
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new IllegalOrderTransitionException("Order Already Processed or completed");
		}

		log.info("payment attempt was '{}', and now '{}'"
				, order.getPaymentAttemptCount(), order.getPaymentAttemptCount() + 1
		);
		order.setPaymentAttemptCount(order.getPaymentAttemptCount() + 1);
		var savedOrder = orderRepository.saveAndFlush(order);

		Session session = paymentService.createCheckoutSession(savedOrder);
		return paymentMapper.toDto(session);
	}

	private OrderItem findItem(Order order, ReserveLine line) {
		return order.getItems().stream()
				.filter(it -> it.getProductId().equals(line.productId()))
				.findFirst().orElseThrow();
	}
}
