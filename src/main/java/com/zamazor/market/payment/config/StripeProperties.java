package com.zamazor.market.payment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.stripe")
public record StripeProperties(
		@NotBlank String publishableKey,
		@NotBlank String secretKey,
		@NotBlank String webhookSecret
) {
}
