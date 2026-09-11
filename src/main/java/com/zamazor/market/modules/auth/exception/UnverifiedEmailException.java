package com.zamazor.market.modules.auth.exception;

public class UnverifiedEmailException extends RuntimeException {
	public UnverifiedEmailException() {
		super("Email address must be verified to complete this action");
	}
}
