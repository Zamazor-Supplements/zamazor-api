package com.zamazor.market.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.Instant;

@ConfigurationProperties(prefix = "application.shop.order-policy")
public record OrderPolicyProperties(Duration cancelWindow, Duration refundWindow, Duration paymentHold) {
	public boolean cancelExpired(Instant createdAt, Instant now) {
		return createdAt != null && createdAt.isBefore(now.minus(cancelWindow));
	}

	// For automatic cleanup of abandoned/unpaid PENDING orders
	public boolean paymentHoldExpired(Instant createdAt, Instant now) {
		return createdAt != null && paymentHold != null && createdAt.isBefore(now.minus(paymentHold));
	}

	public boolean refundExpired(Instant paidAt, Instant now) {
		return paidAt == null || paidAt.isBefore(now.minus(refundWindow));
	}
}