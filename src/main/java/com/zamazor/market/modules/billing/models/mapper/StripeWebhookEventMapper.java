package com.zamazor.market.modules.billing.models.mapper;

import com.stripe.model.Event;
import com.zamazor.market.modules.billing.models.entity.StripeWebhookEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StripeWebhookEventMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "eventId", source = "id")
	@Mapping(target = "eventType", source = "type")
	@Mapping(target = "receivedAt", ignore = true)
	@Mapping(target = "orderId", ignore = true)
	@Mapping(target = "outcome", ignore = true)
	@Mapping(target = "failureReason", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "processedAt", ignore = true)
	StripeWebhookEvent received(Event event);
}