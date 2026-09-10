package com.zamazor.market.modules.catalog.service;

import com.zamazor.market.modules.catalog.exception.AddressNotFoundException;
import com.zamazor.market.modules.catalog.models.dto.AddressDto;
import com.zamazor.market.modules.catalog.models.dto.AddressRequest;
import com.zamazor.market.modules.catalog.models.mapper.AddressMapper;
import com.zamazor.market.modules.catalog.repository.AddressRepository;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {
	private final AddressRepository addressRepository;
	private final AddressMapper addressMapper;
	private final UserRepository userRepository;

	@Transactional
	public AddressDto createOrUpdate(User user, AddressRequest request) {
		return addressRepository.findByUserId(user.getId())
				.map(existingAddress -> {
					addressMapper.update(request, existingAddress);
					return addressMapper.toDto(addressRepository.save(existingAddress));
				}).orElseGet(() -> {
					var address = addressMapper.toEntity(request);
					user.setAddress(address);
					userRepository.save(user);

					return addressMapper.toDto(addressRepository.save(address));
				});
	}

	@Transactional(readOnly = true)
	public AddressDto getDefaultAddress(UUID userId) {
		return addressRepository.findByUserId(userId)
				.map(addressMapper::toDto)
				.orElseThrow(AddressNotFoundException::new);
	}

	@Transactional
	public AddressDto update(User user, AddressRequest request) {
		var existingAddress = addressRepository.findByUserId(user.getId())
				.orElseThrow(AddressNotFoundException::new);

		addressMapper.update(request, existingAddress);

		return addressMapper.toDto(addressRepository.save(existingAddress));
	}
}