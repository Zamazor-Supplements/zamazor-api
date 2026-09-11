package com.zamazor.market.modules.catalog.exception;

public class OrderCancellationException extends RuntimeException {
	public OrderCancellationException(String message) {
		super(message);
	}
}
