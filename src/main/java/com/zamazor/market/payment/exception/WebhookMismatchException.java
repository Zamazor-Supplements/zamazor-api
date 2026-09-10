package com.zamazor.market.payment.exception;

public class WebhookMismatchException extends RuntimeException {
	public WebhookMismatchException(String message) {
		super(message);
	}
}
