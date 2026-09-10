package com.zamazor.market.modules.billing.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zamazor.market.payment.config.StripeProperties;
import com.zamazor.market.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class StripeWebhookController {
	private final StripeProperties stripeProperties;
	private final WebhookService webhookService;

	@PostMapping(value = "/stripe")
	public ResponseEntity<Void> handle(
			@RequestHeader(value = "Stripe-Signature", required = false) String signature,
			@RequestBody String rawBody
	) {
		Event event;
		try {
			event = Webhook.constructEvent(
					rawBody,
					requireSignature(signature),
					stripeProperties.webhookSecret());
		} catch (SignatureVerificationException e) {
			log.warn("Rejected Stripe webhook — invalid signature: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		} catch (IllegalArgumentException e) {
			log.warn("Rejected Stripe webhook — malformed payload: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		}

		try {
			webhookService.dispatch(event);
			return ResponseEntity.ok().build();
		} catch (RuntimeException retryable) {
			log.error("Stripe event {} failed — returning 503 to trigger Stripe retry",
					event.getId(), retryable);
			return ResponseEntity.status(503).build();
		}
	}

	private static String requireSignature(String s) {
		if (s == null || s.isBlank()) {
			throw new IllegalArgumentException("missing Stripe-Signature header");
		}
		return s;
	}
}