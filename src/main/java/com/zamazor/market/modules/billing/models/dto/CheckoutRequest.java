package com.zamazor.market.modules.billing.models.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
		@NotBlank String country,
		@NotBlank String city,
		@NotBlank String street,
		@NotBlank String phone,
		boolean isDefault
) {
}