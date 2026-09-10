package com.zamazor.market.modules.wishlist.service;

import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.wishlist.models.dto.WishlistDto;
import com.zamazor.market.modules.wishlist.models.entity.Wishlist;
import com.zamazor.market.modules.wishlist.models.mapper.WishlistItemMapper;
import com.zamazor.market.modules.wishlist.models.mapper.WishlistMapper;
import com.zamazor.market.modules.wishlist.repository.WishlistRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {
	private final WishlistRepository wishlistRepository;
	private final ProductRepository productRepository;
	private final WishlistMapper wishlistMapper;
	private final WishlistItemMapper wishlistItemMapper;

	public WishlistDto getUserWishlist(UUID userId) {
		List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
		return wishlistMapper.toWishlistDto(wishlists);
	}

	@Transactional
	public WishlistDto mergeWishlist(User user, List<UUID> productIds) {
		if (productIds == null || productIds.isEmpty())
			return getUserWishlist(user.getId());

		Set<UUID> existingProductIds = wishlistRepository
				.findAllProductIdsByUserId(user.getId());
		List<UUID> newProductIds = productIds.stream()
				.filter(productId -> !existingProductIds.contains(productId))
				.distinct()
				.toList();

		if (!newProductIds.isEmpty()) {
			List<Product> productsToSave = productRepository.findAllById(newProductIds);

			List<Wishlist> newWishlistEntities = productsToSave.stream()
					.map(product -> wishlistItemMapper.toEntity(user, product))
					.toList();

			wishlistRepository.saveAll(newWishlistEntities);
		}

		return getUserWishlist(user.getId());
	}


	@Transactional
	public WishlistDto toggleToWishlist(User user, UUID productId) {
		if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
			wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
			return getUserWishlist(user.getId());
		}

		var product = productRepository.findById(productId)
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));

		wishlistRepository.save(wishlistItemMapper.toEntity(user, product));

		return getUserWishlist(user.getId());
	}

	@Transactional
	public WishlistDto removeFromWishlist(UUID userId, UUID productId) {
		var wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
				.orElseThrow(() -> new EntityNotFoundException("Item not found in wishlist"));

		wishlistRepository.delete(wishlist);
		return getUserWishlist(userId);
	}

	@Transactional
	public void clearWishlist(UUID userId) {
		wishlistRepository.deleteByUserId(userId);
	}
}