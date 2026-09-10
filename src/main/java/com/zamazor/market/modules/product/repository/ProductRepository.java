package com.zamazor.market.modules.product.repository;

import com.zamazor.market.modules.product.models.entity.Product;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
	@EntityGraph(attributePaths = {"category"})
	@Override
	@NonNull Optional<Product> findById(@NonNull UUID id);

	@EntityGraph(attributePaths = {"category"})
	@Override
	@NonNull Page<Product> findAll(@NonNull Pageable pageable);

	@EntityGraph(attributePaths = {"category"})
	@Override
	@NonNull Page<Product> findAll(@NonNull Specification<Product> specification, @NonNull Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Product p
			   SET p.stockQuantity   = p.stockQuantity - :qty,
			       p.reservedQuantity = p.reservedQuantity + :qty,
			       p.version          = p.version + 1
			 WHERE p.id = :id AND p.stockQuantity >= :qty
			""")
	int reserveStock(@Param("id") UUID id, @Param("qty") int qty);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Product p
			   SET p.stockQuantity   = p.stockQuantity + :qty,
			       p.reservedQuantity = p.reservedQuantity - :qty,
			       p.version          = p.version + 1
			 WHERE p.id = :id AND p.reservedQuantity >= :qty
			""")
	int releaseStock(@Param("id") UUID id, @Param("qty") int qty);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Product p
			   SET p.reservedQuantity = p.reservedQuantity - :qty,
			       p.version          = p.version + 1
			 WHERE p.id = :id AND p.reservedQuantity >= :qty
			""")
	int confirmReservation(@Param("id") UUID id, @Param("qty") int qty);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE products
			   SET stock_quantity    = stock_quantity + :qty,
			       reserved_quantity = GREATEST(reserved_quantity - :qty, 0),
			       version           = version + 1
			 WHERE id = :id
			""", nativeQuery = true)
	int restoreAvailability(@Param("id") UUID id, @Param("qty") int qty);

	@EntityGraph(attributePaths = {"category"})
	@NonNull Page<Product> findByIdIn(@NonNull Collection<UUID> ids, @NonNull Pageable pageable);

	@EntityGraph(attributePaths = {"category"})
	Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

	interface MetricsSummary {
		long getTotalProducts();

		long getTotalCategories();

		Double getAveragePrice();
	}

	@Query("SELECT COUNT(p) as totalProducts, COUNT(DISTINCT p.category.id) as totalCategories, AVG(p.price) as averagePrice FROM Product p")
	MetricsSummary getMetricsSummary();

	@Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity <= :threshold")
	long countLowStockProducts(@Param("threshold") int threshold);
}