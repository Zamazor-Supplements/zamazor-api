package com.zamazor.market.modules.product.models.mapper;

import com.zamazor.market.modules.product.models.dto.CreateProductRequest;
import com.zamazor.market.modules.product.models.dto.ProductDto;
import com.zamazor.market.modules.product.models.dto.UpdateProductRequest;
import com.zamazor.market.modules.product.models.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper {
	ProductDto toDto(Product product);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "imagePublicId", ignore = true)
	@Mapping(target = "reservedQuantity", ignore = true)
	@Mapping(target = "store", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "cartItems", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	Product toEntity(CreateProductRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "imagePublicId", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "reservedQuantity", ignore = true)
	@Mapping(target = "store", ignore = true)
	@Mapping(target = "cartItems", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	void update(UpdateProductRequest request, @MappingTarget Product product);
}
