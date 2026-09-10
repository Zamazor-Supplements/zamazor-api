package com.zamazor.market.modules.product.specification;

import com.zamazor.market.modules.product.models.entity.Product;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecifications {

	@Contract(pure = true)
	public static @NonNull Specification<Product> createSpec(String q, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (q != null && !q.isBlank()) {
				String pattern = "%" + q.toLowerCase() + "%";
				Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
				Predicate descLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
				predicates.add(criteriaBuilder.or(nameLike, descLike));
			}

			if (categoryId != null) {
				predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
			}

			if (minPrice != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
			}

			if (maxPrice != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}