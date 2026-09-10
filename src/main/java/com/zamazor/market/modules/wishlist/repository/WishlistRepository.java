package com.zamazor.market.modules.wishlist.repository;

import com.zamazor.market.modules.wishlist.models.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
	@EntityGraph(attributePaths = {"product"})
	List<Wishlist> findByUserId(UUID userId);

	boolean existsByUserIdAndProductId(UUID userId, UUID productId);

	@EntityGraph(attributePaths = {"product"})
	Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

	@Modifying
	@Transactional
	@Query("DELETE FROM Wishlist w WHERE w.user.id = :userId")
	void deleteByUserId(@Param("userId") UUID userId);

	@Modifying
	@Transactional
	@Query("DELETE FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
	void deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);

	@Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId")
	Set<UUID> findAllProductIdsByUserId(@Param("userId") UUID userId);
}
