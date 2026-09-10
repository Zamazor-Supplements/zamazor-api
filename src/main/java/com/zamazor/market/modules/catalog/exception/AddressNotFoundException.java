package com.zamazor.market.modules.catalog.exception;

import com.zamazor.market.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AddressNotFoundException extends DomainException {
	public AddressNotFoundException() {
		super("No default address set for this user", HttpStatus.NOT_FOUND, "Address Not Found");
	}
}