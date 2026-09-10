package com.zamazor.market.modules.product.controller;

import com.zamazor.market.modules.product.models.dto.CategoryDto;
import com.zamazor.market.modules.product.models.dto.CategoryRequest;
import com.zamazor.market.modules.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RequestMapping("/categories")
@RestController
@RequiredArgsConstructor
public class CategoryController {
	private final CategoryService categoryService;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest request, UriComponentsBuilder uriBuilder) {
		CategoryDto response = categoryService.create(request);
		var uri = uriBuilder.path("/categories/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(uri).body(response);
	}

	@GetMapping
	public ResponseEntity<List<CategoryDto>> getAll() {
		return ResponseEntity.ok(categoryService.getAll());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}")
	public ResponseEntity<CategoryDto> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.ok(categoryService.update(id, request));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		categoryService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
