package com.zamazor.market.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {
	private final HttpStatus status;
	private final String title;

	protected DomainException(String message, HttpStatus status, String title) {
		super(message);
		this.status = status;
		this.title = title;
	}
}