package com.zamazor.market.modules.auth.exception;

public class TokenInvalidException extends RuntimeException {
	public TokenInvalidException() {
		super("The token is invalid, expired, or has already been used.");
	}
}
