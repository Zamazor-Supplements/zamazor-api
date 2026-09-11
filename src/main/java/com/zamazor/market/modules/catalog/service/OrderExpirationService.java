package com.zamazor.market.modules.catalog.service;

import com.zamazor.market.modules.billing.models.entity.PaymentStatus;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.payment.config.OrderPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderExpirationService {
	private final OrderRepository orderRepository;
	private final OrderService orderService;
	private final OrderPolicyProperties policy;
	private final Clock clock;

	@Transactional
	public void expireStaleReservations() {
		var cutoff = Instant.now(clock).minus(policy.paymentHold());
		var eligibleOrders = orderRepository.findOrdersEligibleForCancellation(cutoff, 50);

		for (var order : eligibleOrders) {
			orderService.cancelOrder(order.getId(), PaymentStatus.EXPIRED);
		}
	}
}