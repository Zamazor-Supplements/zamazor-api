package com.zamazor.market.modules.product.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CategoryNotFoundException extends DomainException {
	public CategoryNotFoundException(UUID id) {
		super("Category not found with id: %s".formatted(id), HttpStatus.NOT_FOUND, "Category Not Found");
	}
}