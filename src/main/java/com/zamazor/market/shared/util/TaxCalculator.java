package com.zamazor.market.shared.util;

import com.zamazor.market.modules.catalog.models.entity.Address;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TaxCalculator {
	public BigDecimal calculate(BigDecimal amount, Address address) {
		return BigDecimal.ZERO;
	}
}
