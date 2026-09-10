package com.zamazor.market.modules.wishlist.models.dto;

import java.util.List;

public record WishlistDto(
		List<WishlistItemDto> items
) {
}