package com.zamazor.market.modules.user.models.dto;

import com.zamazor.market.modules.catalog.models.dto.AddressDto;
import com.zamazor.market.modules.user.models.entity.Role;

import java.util.UUID;

public record UserDto(
		UUID id,
		String email,
		String fullName,
		AddressDto address,
		Role role,
		boolean emailVerified
) {
}