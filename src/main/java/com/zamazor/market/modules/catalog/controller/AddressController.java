package com.zamazor.market.modules.catalog.controller;

import com.zamazor.market.modules.catalog.models.dto.AddressDto;
import com.zamazor.market.modules.catalog.models.dto.AddressRequest;
import com.zamazor.market.modules.catalog.service.AddressService;
import com.zamazor.market.modules.user.models.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {
	private final AddressService addressService;

	@PostMapping
	public ResponseEntity<AddressDto> create(
			@AuthenticationPrincipal User user,
			@Valid @RequestBody AddressRequest request
	) {
		AddressDto response = addressService.createOrUpdate(user, request);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<AddressDto> getDefaultAddress(@AuthenticationPrincipal User user) {
		var response = addressService.getDefaultAddress(user.getId());
		return ResponseEntity.ok(response);
	}

	@PutMapping
	public ResponseEntity<AddressDto> update(
			@AuthenticationPrincipal User user,
			@Valid @RequestBody AddressRequest request
	) {
		var response = addressService.update(user, request);
		return ResponseEntity.ok(response);
	}
}