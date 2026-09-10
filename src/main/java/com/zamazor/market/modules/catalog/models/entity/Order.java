package com.zamazor.market.modules.catalog.models.entity;

import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.catalog.models.dto.StockRestoreDto;
import com.zamazor.market.modules.product.exception.OrderCancellationException;
import com.zamazor.market.modules.product.exception.OrderRefundException;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.payment.config.OrderPolicyProperties;
import com.zamazor.market.shared.model.dto.PricingResult;
import com.zamazor.market.shared.util.OrderStateMachine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Setter
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false)
	private PaymentStatus paymentStatus = PaymentStatus.PENDING;

	@Column(name = "stripe_checkout_session_id", unique = true)
	private String stripeCheckoutSessionId;

	@Column(name = "stripe_payment_intent_id")
	private String stripePaymentIntentId;

	@Column(name = "payment_attempt_count", nullable = false)
	private int paymentAttemptCount = 0;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal subtotal;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal tax = BigDecimal.ZERO;

	@Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
	private BigDecimal shippingCost = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal discount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	@Column(name = "refunded_amount", precision = 10, scale = 2, nullable = false)
	private BigDecimal refundedAmount = BigDecimal.ZERO;

	@Column(name = "refunded_at")
	private Instant refundedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@BatchSize(size = 50)
	private List<OrderItem> items = new ArrayList<>();

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
			@AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
			@AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
			@AttributeOverride(name = "phone", column = @Column(name = "recipient_phone"))
	})
	private AddressComponent shippingAddressSnapshot;

	@CreatedDate
	private Instant createdAt;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Version
	private Long version;

	public void markPaid(String paymentIntentId, Instant now) {
		if (status == OrderStatus.CONFIRMED) return;
		OrderStateMachine.verify(status, OrderStatus.CANCELED);

		this.status = OrderStatus.CONFIRMED;
		this.paymentStatus = PaymentStatus.PAID;
		this.stripePaymentIntentId = paymentIntentId;
		this.paidAt = now;
	}

	public void markPaymentFailedOrCanceled(String eventType) {
		if (status == OrderStatus.CANCELED
				&& (paymentStatus == PaymentStatus.CANCELED || paymentStatus == PaymentStatus.FAILED)) {
			return;
		}
		OrderStateMachine.verify(status, OrderStatus.CANCELED);

		this.status = OrderStatus.CANCELED;
		this.paymentStatus = "payment_intent.canceled".equals(eventType)
				? PaymentStatus.CANCELED
				: PaymentStatus.FAILED;
	}

	public List<StockRestoreDto> recordRefundApplied(BigDecimal refunded, Instant now) {
		if (refunded == null) return List.of();
		if (this.refundedAmount != null && refunded.compareTo(this.refundedAmount) <= 0) return List.of();

		this.refundedAmount = refunded;

		// If it transitions to full refund for the first time, return items to restore
		if (this.status != OrderStatus.REFUNDED && this.refundedAmount.compareTo(this.total) >= 0) {
			OrderStateMachine.verify(status, OrderStatus.REFUNDED);
			this.status = OrderStatus.REFUNDED;
			this.paymentStatus = PaymentStatus.REFUNDED;
			this.refundedAt = now;
			return stockRestore();
		} else if (this.refundedAmount.compareTo(this.total) >= 0) {
			this.status = OrderStatus.REFUNDED;
			this.paymentStatus = PaymentStatus.REFUNDED;
			this.refundedAt = now;
		} else {
			this.paymentStatus = PaymentStatus.PARTIALLY_REFUNDED;
		}

		return List.of();
	}

	public List<StockRestoreDto> cancel(Instant now, OrderPolicyProperties policy) {
		if (this.status == OrderStatus.CANCELED) return List.of();
		if (OrderStateMachine.isTerminal(status)) {
			throw new OrderCancellationException("Cannot cancel a finalized order.");
		}

		// Only enforce the strict cancelWindow if the order is past PENDING (e.g., CONFIRMED)
		if (this.status == OrderStatus.CONFIRMED && policy.cancelExpired(createdAt, now)) {
			throw new OrderCancellationException(
					"Order cancellation window of %d days has expired."
							.formatted(policy.cancelWindow().toDays()));
		}

		// For PENDING orders, you might want to check paymentHold instead:
//		if (this.status == OrderStatus.PENDING && policy.paymentHoldExpired(createdAt, now)) {
//			// Handle pending order expiration
//		}

		OrderStateMachine.verify(status, OrderStatus.CANCELED);
		this.status = OrderStatus.CANCELED;
		return stockRestore();
	}

	public List<StockRestoreDto> refund(Instant now, OrderPolicyProperties policy) {
		if (this.status == OrderStatus.REFUNDED) return List.of();
		if (status != OrderStatus.CONFIRMED && status != OrderStatus.SHIPPED && status != OrderStatus.DELIVERED) {
			throw new OrderRefundException("Only confirmed, shipped or delivered orders can be refunded.");
		}
		if (paymentStatus != PaymentStatus.PAID || paidAt == null) {
			throw new OrderRefundException("Cannot refund an order that was never paid.");
		}
		if (policy.refundExpired(paidAt, now)) {
			throw new OrderRefundException(
					"Refund window of %d days from payment has expired."
							.formatted(policy.refundWindow().toDays()));
		}
		OrderStateMachine.verify(status, OrderStatus.REFUNDED);
		this.paymentStatus = PaymentStatus.REFUNDED;
		this.status = OrderStatus.REFUNDED;
		this.refundedAt = now;
		return stockRestore();
	}

	public static Order createFromCart(Cart cart, PricingResult pricing) {
		var order = new Order();
		order.setUser(cart.getUser());
		order.setSubtotal(pricing.subtotal());
		order.setDiscount(pricing.discount());
		order.setTax(pricing.tax());
		order.setShippingCost(pricing.shipping());
		order.setTotal(pricing.total());

		for (CartItem item : cart.getItems()) {
			var product = item.getProduct();

			var line = new OrderItem();
			line.setOrder(order);
			line.setProductId(product.getId());
			line.setProductName(product.getName());
			line.setProductImageUrl(product.getImageUrl());
			line.setUnitPrice(product.getPrice());
			line.setQuantity(item.getQuantity());

			order.addOrderItem(line);
		}
		return order;
	}

	public void addOrderItem(OrderItem line) {
		this.items.add(line);
		line.setOrder(this);
	}

	private List<StockRestoreDto> stockRestore() {
		return this.items.stream()
				.map(i -> new StockRestoreDto(i.getProductId(), i.getQuantity()))
				.toList();
	}

	public void transitionTo(OrderStatus newStatus) {
		if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.DELIVERED) {
			throw new IllegalOrderTransitionException(
					"Manual transitions are limited to SHIPPED/DELIVERED; CONFIRMED goes through payment");
		}
		if (paymentStatus != PaymentStatus.PAID) {
			throw new IllegalOrderTransitionException("Cannot fulfill an unpaid order");
		}
		OrderStateMachine.verify(this.status, newStatus);
		this.status = newStatus;
	}

	public boolean acceptsPayment() {
		return status == OrderStatus.PENDING;
	}
}