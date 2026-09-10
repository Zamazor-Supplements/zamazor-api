package com.zamazor.market.modules.catalog.models.mapper;

import com.zamazor.market.modules.catalog.models.entity.Cart;
import com.zamazor.market.modules.catalog.models.dto.CartDto;
import com.zamazor.market.shared.model.dto.PricingResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CartItemMapper.class})
public interface CartMapper {
	@Mapping(target = "id", source = "cart.id")
	@Mapping(target = "items", source = "cart.items")
	@Mapping(target = "subtotal", source = "pricing.subtotal")
	@Mapping(target = "tax", source = "pricing.tax")
	@Mapping(target = "shipping", source = "pricing.shipping")
	@Mapping(target = "discount", source = "pricing.discount")
	@Mapping(target = "total", source = "pricing.total")
	CartDto toDto(Cart cart, PricingResult pricing);
}
