package com.zamazor.market.shared.util;

import com.zamazor.market.modules.catalog.models.entity.Cart;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DiscountCalculator {
	public BigDecimal calculate(Cart cart) {
		return cart.getSubtotal().multiply(BigDecimal.ZERO);
	}
}
