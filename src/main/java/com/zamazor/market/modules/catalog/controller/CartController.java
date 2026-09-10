package com.zamazor.market.modules.catalog.controller;

import com.zamazor.market.modules.catalog.models.dto.*;
import com.zamazor.market.modules.catalog.service.CartService;
import com.zamazor.market.modules.user.models.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartController {
	private final CartService cartService;

	@PostMapping("/sync")
	public ResponseEntity<CartDto> mergeCart(@AuthenticationPrincipal User user, @Valid @RequestBody CartRequest request) {
		return ResponseEntity.ok(cartService.mergeCart(user, request.items()));
	}

	@GetMapping
	public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal User user) {
		return ResponseEntity.ok(cartService.getCartByUserId(user));
	}

	@PostMapping("/items")
	public ResponseEntity<CartDto> addItemToCart(@AuthenticationPrincipal User user, @Valid @RequestBody AddToCartRequest request) {
		return ResponseEntity.ok(cartService.addItemToCart(user, request));
	}

	@PatchMapping("/items/{productId}")
	public ResponseEntity<CartDto> updateItemQuantity(
			@AuthenticationPrincipal User user,
			@PathVariable UUID productId,
			@Valid @RequestBody UpdateQuantityRequest request
	) {
		return ResponseEntity.ok(cartService.updateItemQuantity(user, productId, request.quantity()));
	}

	@DeleteMapping("/items/{productId}")
	public ResponseEntity<CartDto> removeItemFromCart(@AuthenticationPrincipal User user, @PathVariable UUID productId) {
		return ResponseEntity.ok(cartService.removeItem(user, productId));
	}

	@DeleteMapping
	public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
		cartService.clearCart(user.getId());
		return ResponseEntity.noContent().build();
	}
}
