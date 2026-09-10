package com.zamazor.market.modules.catalog.service;

import com.zamazor.market.modules.catalog.exception.CartItemNotFoundException;
import com.zamazor.market.modules.catalog.exception.CartNotFoundException;
import com.zamazor.market.modules.catalog.models.dto.*;
import com.zamazor.market.modules.catalog.models.entity.Cart;
import com.zamazor.market.modules.catalog.models.mapper.CartMapper;
import com.zamazor.market.modules.catalog.repository.CartItemRepository;
import com.zamazor.market.modules.catalog.repository.CartRepository;
import com.zamazor.market.modules.product.exception.ProductNotFoundException;
import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.user.repository.UserRepository;
import com.zamazor.market.shared.model.dto.PricingResult;
import com.zamazor.market.shared.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class CartService {
	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final CartItemRepository cartItemRepository;
	private final CartMapper cartMapper;
	private final PricingService pricingService;

	@Transactional
	public CartDto mergeCart(User user, List<GuestCartItemDto> items) {
		Cart cart = cartRepository.findByUserId(user.getId())
				.orElseGet(() -> {
					Cart newCart = new Cart();
					user.setCart(newCart);
					userRepository.saveAndFlush(user); // Cascades or saves safely
					return newCart;
				});

		if (items == null || items.isEmpty()) {
			return cartMapper.toDto(cart, pricingService.calculate(cart, user));
		}

		// Extract products in a single batch query
		List<UUID> productIds = items.stream().map(GuestCartItemDto::productId).toList();
		Map<UUID, Product> productMap = productRepository.findAllById(productIds).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		cart.mergeGuestCart(items, productMap);

		PricingResult pricing = pricingService.calculate(cart, user);
		return cartMapper.toDto(cartRepository.save(cart), pricing);
	}

	public CartDto getCartByUserId(User user) {
		return cartRepository.findByUserId(user.getId())
				.map(cart -> {
					PricingResult pricing = pricingService.calculate(cart, user);
					return cartMapper.toDto(cart, pricing);
				})
				.orElseGet(() -> new CartDto(
						null,
						Collections.emptyList(),
						BigDecimal.ZERO,
						null,
						null,
						null,
						BigDecimal.ZERO
				));
	}

	@Transactional
	public CartDto addItemToCart(User user, AddToCartRequest request) {
		var cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
			var newCart = new Cart();
			newCart = cartRepository.saveAndFlush(newCart);
			user.setCart(newCart);

			userRepository.saveAndFlush(user);
			return newCart;
		});

		var product = productRepository.findById(request.productId())
				.orElseThrow(() -> new ProductNotFoundException(request.productId()));

		cart.addProduct(product, request.quantity());

		PricingResult pricing = pricingService.calculate(cart, user);
		return cartMapper.toDto(cartRepository.saveAndFlush(cart), pricing);
	}

	@Transactional
	public CartDto updateItemQuantity(User user, UUID productId, Integer quantity) {
		var cart = cartRepository.findByUserId(user.getId())
				.orElseThrow(CartNotFoundException::new);
		var product = productRepository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));

		cart.updateItemQuantity(product, quantity);

		PricingResult pricing = pricingService.calculate(cart, user);
		return cartMapper.toDto(cartRepository.save(cart), pricing);
	}

	@Transactional
	public CartDto removeItem(User user, UUID productId) {
		var cart = cartRepository.findByUserId(user.getId())
				.orElseThrow(CartNotFoundException::new);
		if (!cartItemRepository.existsByCartIdAndProductId(cart.getId(), productId)) {
			throw new CartItemNotFoundException();
		}

		cart.removeItem(productId);

		PricingResult pricing = pricingService.calculate(cart, user);
		return cartMapper.toDto(cartRepository.save(cart), pricing);
	}

	@Transactional
	public void clearCart(UUID userId) {
		var cart = cartRepository.findByUserId(userId)
				.orElseThrow(CartNotFoundException::new);

		cart.clear();
	}
}