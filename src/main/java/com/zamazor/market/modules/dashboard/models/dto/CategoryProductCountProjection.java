package com.zamazor.market.modules.dashboard.models.dto;

public interface CategoryProductCountProjection {
	String getId();

	String getLabel();

	long getProductCount();
}