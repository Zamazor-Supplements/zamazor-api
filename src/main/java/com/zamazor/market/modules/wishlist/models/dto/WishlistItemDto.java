package com.zamazor.market.modules.wishlist.models.dto;

import com.zamazor.market.modules.product.models.dto.ProductDto;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemDto(
		UUID id,
		ProductDto product,
		Instant createdAt
) {
}
