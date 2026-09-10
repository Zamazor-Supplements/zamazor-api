package com.zamazor.market.modules.catalog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalOrderTransitionException extends RuntimeException {
	public IllegalOrderTransitionException(String message) {
		super(message);
	}
}
