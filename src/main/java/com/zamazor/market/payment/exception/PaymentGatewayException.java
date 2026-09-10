package com.zamazor.market.payment.exception;

public class PaymentGatewayException extends RuntimeException {
	public PaymentGatewayException(String message) {
		super(message);
	}
}