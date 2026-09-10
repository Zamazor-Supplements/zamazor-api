package com.zamazor.market.modules.billing.repository;

import com.zamazor.market.modules.billing.models.entity.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
	boolean existsByEventId(String eventId);
}
