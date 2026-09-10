package com.zamazor.market.modules.user.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends DomainException {
	public UserNotFoundException(UUID id) {
		super("User not found with id: %s".formatted(id), HttpStatus.NOT_FOUND, "User Not Found");
	}
}