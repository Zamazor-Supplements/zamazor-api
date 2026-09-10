package com.zamazor.market.modules.catalog.repository;

import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
	@EntityGraph(attributePaths = {"items"})
	@NonNull Page<Order> findAll(@NonNull Specification<Order> specification, @NonNull Pageable pageable);

	@EntityGraph(attributePaths = {"items"})
	Page<Order> findByUserId(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = {"items", "user"})
	@NonNull Optional<Order> findById(@NonNull UUID id);

	Optional<Order> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

	Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.stripePaymentIntentId = :paymentIntentId")
	Optional<Order> findByStripePaymentIntentIdForUpdate(@Param("paymentIntentId") String paymentIntentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Order o
			   SET o.status = :target
			 WHERE o.id = :id AND o.status = :expected
			""")
	int transitionStatus(
			@Param("id") UUID id,
			@Param("expected") OrderStatus expected,
			@Param("target") OrderStatus target
	);

	// OrderRepository — claim with SKIP LOCKED so multiple app instances never double-process
	@Query(value = """
			SELECT * FROM orders
			 WHERE payment_status = 'PENDING' AND created_at < :cutoff
			 ORDER BY created_at
			 FOR UPDATE SKIP LOCKED
			 LIMIT :batch
			""", nativeQuery = true)
	List<Order> findExpiredPending(@Param("cutoff") Instant cutoff, @Param("batch") int batch);
}
