package com.zamazor.market.mail.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.mail")
public record MailProperties(
		String personalName,
		@Valid @Email @NotBlank String fromAddress
) {
}
