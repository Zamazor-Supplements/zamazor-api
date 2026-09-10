package com.zamazor.market.modules.catalog.models.dto;

import java.util.UUID;

public record UserMinDto(
		UUID id,
		String email,
		String fullName,
		String role
) {
}
