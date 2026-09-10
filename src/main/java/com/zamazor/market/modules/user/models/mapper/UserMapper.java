package com.zamazor.market.modules.user.models.mapper;

import com.zamazor.market.modules.auth.models.dto.RegisterRequest;
import com.zamazor.market.modules.catalog.models.mapper.AddressMapper;
import com.zamazor.market.modules.user.models.dto.UserDto;
import com.zamazor.market.modules.user.models.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface UserMapper {
	UserDto toDto(User user);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "isAdmin", ignore = true)
	@Mapping(target = "cart", ignore = true)
	@Mapping(target = "orders", ignore = true)
	@Mapping(target = "address", ignore = true)
	@Mapping(target = "wishlists", ignore = true)
	User toEntity(RegisterRequest request);
}
