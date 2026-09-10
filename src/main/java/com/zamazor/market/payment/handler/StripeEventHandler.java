package com.zamazor.market.payment.handler;

import com.stripe.model.Event;

import java.util.Set;
import java.util.UUID;

public interface StripeEventHandler {
	/**
	 * Stripe {@code type} strings this handler accepts.
	 */
	Set<String> eventTypes();

	default boolean supports(String type) {
		return eventTypes().contains(type);
	}

	/**
	 * Process the event idempotently. Implementations MUST be safe to re-run:
	 * the router dedupes by event id, and domain methods are monotonic.
	 *
	 * @return the order id this event was applied to, or {@code null} if none
	 * @throws com.zamazor.market.payment.exception.WebhookMismatchException permanent mismatch (recorded, not retried)
	 * @throws RuntimeException                                              transient failure (propagated → Stripe retries)
	 */
	UUID handle(Event event);
}
