package com.zamazor.market.modules.billing.models.entity;

public enum PaymentStatus {
	PENDING,
	AUTHORIZED,
	PAID,
	PARTIALLY_REFUNDED,
	REFUNDED,
	CANCELED,
	FAILED,
	EXPIRED
}
