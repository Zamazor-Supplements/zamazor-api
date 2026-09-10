package com.zamazor.market.modules.catalog.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends DomainException {
	public CartItemNotFoundException() {
		super("Product not present in cart", HttpStatus.NOT_FOUND, "CartItem Not Found");
	}
}