package com.zamazor.market.modules.catalog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyCartException extends RuntimeException {
	public EmptyCartException() {
		super("Cannot checkout an empty cart");
	}
}
