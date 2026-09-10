package com.zamazor.market.modules.product.repository;

import com.zamazor.market.modules.dashboard.models.dto.CategoryProductCountProjection;
import com.zamazor.market.modules.product.models.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
	boolean existsByLabel(String label);

	@Query("SELECT c.id AS id, c.label AS label, COUNT(p) AS productCount FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.label")
	List<CategoryProductCountProjection> findAllWithProductCounts();
}
