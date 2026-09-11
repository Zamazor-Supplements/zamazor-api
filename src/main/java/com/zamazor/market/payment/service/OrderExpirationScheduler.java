package com.zamazor.market.payment.service;

import com.zamazor.market.modules.catalog.service.OrderExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {
	private final OrderExpirationService orderExpirationService;

	@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
	public void expireStaleReservations() {
		orderExpirationService.expireStaleReservations();
	}
}
