package com.zamazor.market.modules.wishlist.models.mapper;

import com.zamazor.market.modules.wishlist.models.dto.WishlistDto;
import com.zamazor.market.modules.wishlist.models.dto.WishlistItemDto;
import com.zamazor.market.modules.wishlist.models.entity.Wishlist;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WishlistMapper {
	default WishlistDto toWishlistDto(List<Wishlist> wishlists) {
		if (wishlists == null) return null;
		return new WishlistDto(toItemDtoList(wishlists));
	}

	List<WishlistItemDto> toItemDtoList(List<Wishlist> wishlists);
}
