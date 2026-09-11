package com.zamazor.market.modules.billing.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "stripe_webhook_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_webhook_event_id",
				columnNames = "event_id"
		)
)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class StripeWebhookEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "event_id")
	private String eventId;

	@Column(name = "event_type")
	private String eventType;

	@Column(name = "order_id")
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false)
	private Outcome outcome = Outcome.RECEIVED;

	@Column(name = "failure_reason")
	private String failureReason;

	@CreatedDate
	private Instant receivedAt;

	@Version
	private Long version;

	private Instant processedAt;

	public void markProcessed(UUID orderId, Instant processedAt) {
		this.orderId = orderId;
		this.outcome = Outcome.PROCESSED;
		this.processedAt = processedAt;
	}

	public void markUnhandled(Instant processedAt) {
		this.outcome = Outcome.UNHANDLED;
		this.processedAt = processedAt;
	}

	public void markRejected(String reason, Instant processedAt) {
		this.outcome = Outcome.REJECTED;
		this.failureReason = truncate(reason);
		this.processedAt = processedAt;
	}

	private static String truncate(String s) {
		return s == null ? null : s.substring(0, Math.min(s.length(), 500));
	}
}