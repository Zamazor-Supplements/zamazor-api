package com.zamazor.market.modules.catalog.models.dto;

import java.util.List;

public record CartRequest(List<GuestCartItemDto> items) {
}