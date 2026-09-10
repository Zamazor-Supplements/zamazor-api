package com.zamazor.market.shared.util;

import com.zamazor.market.modules.catalog.models.entity.Address;
import com.zamazor.market.modules.catalog.models.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ShippingCalculator {
	public BigDecimal calculate(List<CartItem> items, Address address) {
		return BigDecimal.ZERO;
	}
}
