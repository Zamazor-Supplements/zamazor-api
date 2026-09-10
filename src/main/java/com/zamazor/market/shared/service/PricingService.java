package com.zamazor.market.shared.service;

import com.zamazor.market.modules.catalog.models.entity.Cart;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.shared.model.dto.PricingResult;
import com.zamazor.market.shared.util.DiscountCalculator;
import com.zamazor.market.shared.util.ShippingCalculator;
import com.zamazor.market.shared.util.TaxCalculator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class PricingService {
	private final TaxCalculator taxCalculator;
	private final DiscountCalculator discountCalculator;
	private final ShippingCalculator shippingCalculator;

	public PricingResult calculate(Cart cart, User user) {
		var address = user.getAddress();
		BigDecimal subtotal = cart.getSubtotal();
		BigDecimal discount = discountCalculator.calculate(cart);

		BigDecimal taxableAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
		BigDecimal tax = taxCalculator.calculate(taxableAmount, address);

		BigDecimal shipping = shippingCalculator.calculate(cart.getItems(), address);
		BigDecimal total = taxableAmount.add(tax).add(shipping);

		return new PricingResult(subtotal, discount, tax, shipping, total);
	}
}
