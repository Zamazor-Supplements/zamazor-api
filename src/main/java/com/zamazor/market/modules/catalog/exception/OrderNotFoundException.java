package com.zamazor.market.modules.catalog.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends DomainException {
	public OrderNotFoundException(UUID id) {
		super("Order not found with id: %s".formatted(id), HttpStatus.NOT_FOUND, "Order Not Found");
	}
}