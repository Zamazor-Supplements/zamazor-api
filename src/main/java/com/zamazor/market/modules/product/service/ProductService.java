package com.zamazor.market.modules.product.service;

import com.zamazor.market.modules.product.exception.CategoryNotFoundException;
import com.zamazor.market.modules.product.exception.ProductNotFoundException;
import com.zamazor.market.shared.api.PageResponse;
import com.zamazor.market.media.model.StoredMediaMetadata;
import com.zamazor.market.media.ports.MediaStoragePort;
import com.zamazor.market.modules.product.models.dto.CreateProductRequest;
import com.zamazor.market.modules.product.models.dto.ProductDto;
import com.zamazor.market.modules.product.models.dto.UpdateProductRequest;
import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.product.models.mapper.ProductMapper;
import com.zamazor.market.modules.product.repository.CategoryRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import com.zamazor.market.modules.product.repository.StoreRepository;
import com.zamazor.market.modules.product.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final StoreRepository storeRepository;
	private final ProductMapper productMapper;
	private final MediaStoragePort mediaStorage;

	@Transactional
	public ProductDto create(CreateProductRequest request, @NonNull MultipartFile image) throws IOException {
		var product = productMapper.toEntity(request);
		var category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
		var store = storeRepository.findOne()
				.orElseThrow(() -> new IllegalStateException("The single store instance is missing from the database!"));

		product.setCategory(category);
		product.setStore(store);
		StoredMediaMetadata metadata = mediaStorage.upload(image.getInputStream(), image.getOriginalFilename(), "products");
		product.setImageUrl(metadata.secureUrl());
		product.setImagePublicId(metadata.publicId());

		return productMapper.toDto(productRepository.save(product));
	}

	public PageResponse<ProductDto> getByCategory(UUID categoryId, Pageable pageable) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new CategoryNotFoundException(categoryId);
		}

		Page<ProductDto> productPage = productRepository
				.findByCategoryId(categoryId, pageable).map(productMapper::toDto);
		return new PageResponse<>(productPage);
	}

	public PageResponse<ProductDto> search(
			String query,
			UUID categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable
	) {
		Specification<Product> spec = ProductSpecifications.createSpec(query, categoryId, minPrice, maxPrice);
		Page<ProductDto> productPage = productRepository
				.findAll(spec, pageable).map(productMapper::toDto);

		return new PageResponse<>(productPage);
	}

	public PageResponse<ProductDto> bulk(List<UUID> productIds, Pageable pageable) {
		Page<ProductDto> productPage = productRepository.findByIdIn(productIds, pageable)
				.map(productMapper::toDto);

		return new PageResponse<>(productPage);
	}

	public ProductDto getById(UUID id) {
		return productRepository.findById(id)
				.map(productMapper::toDto)
				.orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Transactional
	public ProductDto update(UUID id, @NonNull UpdateProductRequest request, MultipartFile image) throws IOException {
		var product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));

		productMapper.update(request, product);

		if (request.categoryId() != null && !request.categoryId().equals(product.getCategory().getId())) {
			var category = categoryRepository.findById(request.categoryId())
					.orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
			product.setCategory(category);
		}
		if (image != null && image.getSize() > 0) {
			StoredMediaMetadata metadata = mediaStorage.upload(image.getInputStream(), image.getOriginalFilename(), "products");
			product.setImageUrl(metadata.secureUrl());
			product.setImagePublicId(metadata.publicId());
		}

		return productMapper.toDto(product);
	}

	@Transactional
	public void delete(UUID id) {
		if (!productRepository.existsById(id)) {
			throw new ProductNotFoundException(id);
		}
		productRepository.deleteById(id);
	}
}
