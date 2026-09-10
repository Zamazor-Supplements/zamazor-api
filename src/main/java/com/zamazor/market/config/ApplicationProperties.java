package com.zamazor.market.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(
		@Valid @URL @NotBlank String frontendUrl,
		@Valid @URL @NotBlank String backendUrl,
		@Valid @NotBlank String name,
		@Valid @Email @NotBlank String supportEmail,
		@NotBlank String supportPhone
) {
}
