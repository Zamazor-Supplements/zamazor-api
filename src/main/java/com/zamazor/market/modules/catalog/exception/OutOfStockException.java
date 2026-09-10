package com.zamazor.market.modules.catalog.exception;

import com.zamazor.market.modules.product.models.entity.Product;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OutOfStockException extends RuntimeException {
	public OutOfStockException(Product product, int totalRequestedQuantity) {
		super("Insufficient stock for product '%s'. Requested: %d, Available: %d"
				.formatted(product.getName(), totalRequestedQuantity, product.getStockQuantity()));
	}
}
