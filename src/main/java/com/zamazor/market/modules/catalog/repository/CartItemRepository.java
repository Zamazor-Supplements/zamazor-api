package com.zamazor.market.modules.catalog.repository;

import com.zamazor.market.modules.catalog.models.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
	void deleteByProductId(UUID productId);

	boolean existsByCartIdAndProductId(UUID cartId, UUID productId);
}
