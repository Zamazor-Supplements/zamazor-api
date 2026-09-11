package com.zamazor.market.modules.auth.models.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
		@NotBlank String token
) {
}
