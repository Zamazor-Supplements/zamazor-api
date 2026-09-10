package com.zamazor.market.modules.catalog.models.mapper;

import com.zamazor.market.modules.catalog.models.dto.OrderDto;
import com.zamazor.market.modules.catalog.models.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		uses = {OrderItemMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {
	@Mapping(source = "shippingAddressSnapshot.country", target = "shippingCountry")
	@Mapping(source = "shippingAddressSnapshot.city", target = "shippingCity")
	@Mapping(source = "shippingAddressSnapshot.street", target = "shippingStreet")
	@Mapping(source = "shippingAddressSnapshot.phone", target = "phone")
	@Mapping(target = "shippingCost", source = "shippingCost")
	OrderDto toDto(Order order);
}
