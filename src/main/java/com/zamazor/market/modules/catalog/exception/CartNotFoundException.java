package com.zamazor.market.modules.catalog.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CartNotFoundException extends DomainException {
	public CartNotFoundException() {
		super("Cart empty or missing", HttpStatus.NOT_FOUND, "Cart Not Found");
	}
}