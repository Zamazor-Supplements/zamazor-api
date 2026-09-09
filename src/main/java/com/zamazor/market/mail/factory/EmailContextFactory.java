package com.zamazor.market.mail.factory;

import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.modules.catalog.models.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailContextFactory {
	private final ApplicationProperties application;

	public Map<String, Object> create() {
		Map<String, Object> model = new HashMap<>();
		model.put("appName", application.name());
		model.put("supportEmail", application.supportEmail());
		model.put("supportPhone", application.supportPhone());
		model.put("year", Year.now().getValue());
		return model;
	}

	public Map<String, Object> create(Order order) {
		Map<String, Object> model = new HashMap<>(create());
		model.put("orderId", order.getId());
		return model;
	}
}