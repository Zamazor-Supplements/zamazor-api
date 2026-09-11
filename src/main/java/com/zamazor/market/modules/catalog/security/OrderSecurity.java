package com.zamazor.market.modules.catalog.security;

import com.zamazor.market.modules.auth.exception.UnverifiedEmailException;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.user.models.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {
	private final OrderRepository orderRepository;

	public boolean isEmailVerified(Object principal) {
		if (principal instanceof User user) {
			if (!user.isEmailVerified()) {
				throw new UnverifiedEmailException();
			}
			return true;
		}
		throw new AccessDeniedException("Invalid principal or unauthenticated user");
	}

	public boolean isOwnerOrAdmin(UUID orderId, Object principal) {
		if (principal instanceof User user) {
			boolean isAdmin = user.getAuthorities().stream()
					.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
			if (isAdmin) return true;
			if (!isEmailVerified(principal)) return false;
			boolean isOwner = orderRepository.existsByIdAndUserId(orderId, user.getId());
			if (!isOwner) {
				throw new AccessDeniedException("You do not have permission to access this order.");
			}

			return true;
		}
		throw new AccessDeniedException("Invalid principal or unauthenticated user.");
	}
}