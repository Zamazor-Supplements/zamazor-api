package com.zamazor.market.payment.service;

import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.catalog.service.OrderService;
import com.zamazor.market.payment.config.OrderPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentSweeper {
	private final OrderPolicyProperties policy;
	private final OrderRepository orderRepository;
	private final OrderService orderService;

	@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
	public void expireStaleReservations() {
		var cutoff = Instant.now().minus(policy.paymentHold());

		orderRepository.findExpiredPending(cutoff, 50)
				.forEach(order -> orderService.cancelOrder(order.getId(), PaymentStatus.EXPIRED));
	}
}
