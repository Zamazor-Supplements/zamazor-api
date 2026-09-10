package com.zamazor.market.modules.billing.repository;

import java.util.Optional;
import java.util.UUID;

import com.zamazor.market.modules.billing.models.entity.OrderRefund;
import com.zamazor.market.modules.billing.models.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, UUID> {
	Optional<OrderRefund> findByStripeRefundId(String stripeRefundId);

	@Query("select coalesce(sum(r.amount), 0) from OrderRefund r where r.order.id = :orderId and r.status = :status")
	long sumTotalAmountByStatus(@Param("orderId") UUID orderId, @Param("status") RefundStatus status);
}
