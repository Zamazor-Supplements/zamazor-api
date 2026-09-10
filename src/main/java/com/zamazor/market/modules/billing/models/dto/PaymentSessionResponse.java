package com.zamazor.market.modules.billing.models.dto;

import java.time.Instant;

public record PaymentSessionResponse(
		String paymentUrl,
		String sessionId,
		Instant expiresAt
) {
}
