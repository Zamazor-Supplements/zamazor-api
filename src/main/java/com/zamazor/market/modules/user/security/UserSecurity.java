package com.zamazor.market.modules.user.security;

import com.zamazor.market.modules.user.models.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {
	public boolean isUserOrAdmin(UUID id, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Full authentication is required to access this resource");
		}

		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

		if (isAdmin) {
			return true;
		}

		if (authentication.getPrincipal() instanceof User user) {
			if (!user.getId().equals(id)) {
				throw new AccessDeniedException("You do not have permission to access or modify this user profile");
			}
			return true;
		}

		throw new AccessDeniedException("Invalid authentication principal");
	}
}