package com.zamazor.market.modules.billing.models.entity;

import java.time.Instant;
import java.util.UUID;

import com.zamazor.market.modules.catalog.models.entity.Order;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "order_refunds",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_order_refund_stripe_id", columnNames = "stripe_refund_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRefund {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "stripe_refund_id", nullable = false, length = 128, updatable = false)
	private String stripeRefundId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private RefundStatus status = RefundStatus.PENDING;

	@Column(name = "failure_reason", length = 512)
	private String failureReason;

	@Column(name = "stripe_created_at")
	private Instant stripeCreatedAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private Long version;

	public static OrderRefund createdFrom(com.stripe.model.Refund refund, Order order) {
		OrderRefund r = new OrderRefund();
		r.stripeRefundId = refund.getId();
		r.order = order;
		r.amount = refund.getAmount();
		r.stripeCreatedAt = refund.getCreated() != null
				? Instant.ofEpochSecond(refund.getCreated()) : null;
		return r;
	}

	public void markSucceeded() {
		this.status = RefundStatus.SUCCEEDED;
	}

	public void markPending() {
		this.status = RefundStatus.PENDING;
	}

	public void markFailed(String reason) {
		this.status = RefundStatus.FAILED;
		this.failureReason = reason == null ? null
				: reason.substring(0, Math.min(reason.length(), 500));
	}
}
