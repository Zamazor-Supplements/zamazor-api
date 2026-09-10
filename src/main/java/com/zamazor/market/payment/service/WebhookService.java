package com.zamazor.market.payment.service;

import java.util.List;
import java.util.UUID;

import com.zamazor.market.modules.billing.models.entity.StripeWebhookEvent;
import com.zamazor.market.modules.billing.models.mapper.StripeWebhookEventMapper;
import com.zamazor.market.modules.billing.repository.StripeWebhookEventRepository;
import com.zamazor.market.payment.exception.WebhookMismatchException;
import com.zamazor.market.payment.handler.StripeEventHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookService {
	private final List<StripeEventHandler> handlers;
	private final StripeWebhookEventRepository events;
	private final StripeWebhookEventMapper stripeWebhookEventMapper;

	/**
	 * Record-then-dispatch, all in ONE transaction. A transient failure rolls back
	 * the dedupe row so Stripe's retry reprocesses cleanly. Permanent rejections
	 * ({@link WebhookMismatchException}) are recorded as REJECTED and committed —
	 * Stripe stops retrying, we keep the audit trail.
	 */
	@Transactional
	public DispatchOutcome dispatch(Event event) {

		if (events.existsByEventId(event.getId())) {
			log.debug("Stripe event {} already processed — skipping", event.getId());
			return DispatchOutcome.DUPLICATE;
		}

		StripeWebhookEvent record;
		try {
			record = events.saveAndFlush(stripeWebhookEventMapper.received(event));
		} catch (DataIntegrityViolationException race) {
			log.debug("Stripe event {} inserted by a concurrent delivery", event.getId());
			return DispatchOutcome.DUPLICATE;
		}

		StripeEventHandler handler = handlers.stream()
				.filter(h -> h.supports(event.getType()))
				.findFirst()
				.orElse(null);

		if (handler == null) {
			record.markUnhandled();
			log.info("No handler for Stripe event type {}", event.getType());
			return DispatchOutcome.UNHANDLED;
		}

		try {
			UUID orderId = handler.handle(event);
			record.markProcessed(orderId);
			return DispatchOutcome.PROCESSED;
		} catch (ObjectOptimisticLockingFailureException e) {
			log.warn("Stripe event {} optimistic-lock contention (order updated concurrently) — retry will reprocess",
					event.getId(), e);
			throw e;                              // → 503 → Stripe retries
		} catch (WebhookMismatchException e) {
			record.markRejected(e.getMessage());
			log.error("Stripe event {} permanently rejected: {}", event.getId(), e.getMessage());
			return DispatchOutcome.REJECTED;      // 200 — do not retry, keep the record
		} catch (RuntimeException e) {
			// rollback everything (incl. the dedupe row) so the retry reprocesses
			log.error("Stripe event {} failed transiently — will retry", event.getId(), e);
			throw e;                              // → 503 → Stripe retries
		}
	}

	public enum DispatchOutcome {
		PROCESSED, UNHANDLED, DUPLICATE, REJECTED   // all → HTTP 200
		// THROW is represented by the RuntimeException propagating out of dispatch()
	}
}