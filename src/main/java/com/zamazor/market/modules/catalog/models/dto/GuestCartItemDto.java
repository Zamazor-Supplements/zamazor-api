package com.zamazor.market.modules.catalog.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GuestCartItemDto(
		@NotBlank UUID productId,
		@NotNull @Min(1) Integer quantity
) {
}