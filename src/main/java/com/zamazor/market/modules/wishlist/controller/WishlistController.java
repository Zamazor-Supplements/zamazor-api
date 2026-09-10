package com.zamazor.market.modules.wishlist.controller;

import com.zamazor.market.modules.product.models.dto.BulkProductRequest;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.wishlist.models.dto.WishlistDto;
import com.zamazor.market.modules.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
public class WishlistController {
	private final WishlistService wishlistService;

	@GetMapping
	public ResponseEntity<WishlistDto> getWishlist(@AuthenticationPrincipal User user) {
		return ResponseEntity.ok(wishlistService.getUserWishlist(user.getId()));
	}

	@PostMapping("/sync")
	public ResponseEntity<WishlistDto> mergeWishlist(
			@AuthenticationPrincipal User user,
			@Valid @RequestBody BulkProductRequest request
	) {
		return ResponseEntity.ok(wishlistService.mergeWishlist(user, request.ids()));
	}

	@PostMapping("/{productId}")
	public ResponseEntity<WishlistDto> toggleToWishlist(
			@AuthenticationPrincipal User user,
			@PathVariable UUID productId
	) {
		return ResponseEntity.ok(wishlistService.toggleToWishlist(user, productId));
	}

	@DeleteMapping("/{productId}")
	public ResponseEntity<WishlistDto> removeFromWishlist(
			@AuthenticationPrincipal User user,
			@PathVariable UUID productId
	) {
		return ResponseEntity.ok(wishlistService.removeFromWishlist(user.getId(), productId));
	}

	@DeleteMapping
	public ResponseEntity<Void> clearWishlist(@AuthenticationPrincipal User user) {
		wishlistService.clearWishlist(user.getId());
		return ResponseEntity.noContent().build();
	}
}
