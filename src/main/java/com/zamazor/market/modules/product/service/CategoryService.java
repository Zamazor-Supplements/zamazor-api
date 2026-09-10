package com.zamazor.market.modules.product.service;

import com.zamazor.market.modules.product.exception.CategoryNotFoundException;
import com.zamazor.market.modules.product.exception.CategoryAlreadyExistsException;
import com.zamazor.market.modules.product.models.dto.CategoryDto;
import com.zamazor.market.modules.product.models.dto.CategoryRequest;
import com.zamazor.market.modules.product.models.entity.Category;
import com.zamazor.market.modules.product.models.mapper.CategoryMapper;
import com.zamazor.market.modules.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CategoryService {
	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public CategoryDto create(CategoryRequest request) {
		if (categoryRepository.existsByLabel(request.label())) {
			throw new CategoryAlreadyExistsException("Category with label %s already exists".formatted(request.label()));
		}
		var category = new Category();
		category.setLabel(request.label());
		return categoryMapper.toDto(categoryRepository.save(category));
	}

	public List<CategoryDto> getAll() {
		return categoryRepository.findAll().stream().map(categoryMapper::toDto).toList();
	}

	public CategoryDto update(UUID id, CategoryRequest request) {
		var category = categoryRepository.findById(id)
				.orElseThrow(() -> new CategoryNotFoundException(id));

		if (categoryRepository.existsByLabel(request.label())) {
			throw new CategoryAlreadyExistsException("Category with label %s already exists".formatted(request.label()));
		}

		category.setLabel(request.label());
		return categoryMapper.toDto(categoryRepository.save(category));
	}

	public void delete(UUID id) {
		if (!categoryRepository.existsById(id)) {
			throw new CategoryNotFoundException(id);
		}
		categoryRepository.deleteById(id);
	}
}
