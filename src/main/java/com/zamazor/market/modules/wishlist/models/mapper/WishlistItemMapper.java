package com.zamazor.market.modules.wishlist.models.mapper;

import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.product.models.mapper.ProductMapper;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.wishlist.models.dto.WishlistItemDto;
import com.zamazor.market.modules.wishlist.models.entity.Wishlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface WishlistItemMapper {
	WishlistItemDto toDto(Wishlist wishlist);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", source = "user")
	@Mapping(target = "product", source = "product")
	@Mapping(target = "createdAt", ignore = true)
	Wishlist toEntity(User user, Product product);
}