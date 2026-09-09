package com.zamazor.market.modules.dashboard.models.mapper;

import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.dashboard.models.dto.*;
import com.zamazor.market.modules.product.models.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DashboardMapper {
	@Mapping(source = "shippingAddressSnapshot.country", target = "shippingCountry")
	@Mapping(source = "shippingAddressSnapshot.city", target = "shippingCity")
	@Mapping(source = "shippingAddressSnapshot.street", target = "shippingStreet")
	@Mapping(source = "shippingAddressSnapshot.phone", target = "phone")
	@Mapping(target = "shippingCost", source = "shippingCost")
	RecentOrderDto toRecentOrderDto(Order order);

	@Mapping(target = "stockQuantity", source = "stockQuantity")
	@Mapping(target = "category", source = "category.label", defaultValue = "Uncategorized")
	LowStockProductDto toLowStockProductDto(Product product);

	CategoryMetrics toAnalyticsDto(CategoryProductCountProjection projection);

	List<CategoryMetrics> toAnalyticsDtoList(List<CategoryProductCountProjection> projections);
}