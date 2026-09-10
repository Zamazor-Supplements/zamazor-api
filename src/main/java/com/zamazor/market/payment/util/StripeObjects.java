package com.zamazor.market.payment.util;

import java.util.Optional;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;

public final class StripeObjects {
	private StripeObjects() {
	}

	public static <T extends StripeObject> T deserialize(Event event, Class<T> type) {
		EventDataObjectDeserializer d = event.getDataObjectDeserializer();
		Optional<StripeObject> obj = d.getObject();

		if (obj.isPresent() && type.isInstance(obj.get())) {
			return type.cast(obj.get());
		}

		try {
			StripeObject unsafeObj = d.deserializeUnsafe();
			if (type.isInstance(unsafeObj)) {
				return type.cast(unsafeObj);
			}
		} catch (EventDataObjectDeserializationException e) {
			throw new IllegalStateException("Failed to deserialize unsafe object for event %s".formatted(event.getId()), e);
		}

		throw new IllegalStateException("Cannot deserialize %s from event %s"
				.formatted(type.getSimpleName(), event.getId())
		);
	}
}