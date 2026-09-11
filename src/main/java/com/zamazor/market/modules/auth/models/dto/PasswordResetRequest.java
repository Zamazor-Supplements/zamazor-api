package com.zamazor.market.modules.auth.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
		@NotBlank @Email String email
) {
}