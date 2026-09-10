package com.zamazor.market.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StripeConfiguration {
	private final StripeProperties stripeProperties;

	@PostConstruct
	public void init() {
		Stripe.apiKey = stripeProperties.secretKey();
	}
}
