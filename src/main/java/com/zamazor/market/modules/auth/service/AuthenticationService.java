package com.zamazor.market.modules.auth.service;

import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.mail.service.EmailService;
import com.zamazor.market.security.crypto.JwtService;
import com.zamazor.market.modules.auth.exception.EmailAlreadyInUseException;
import com.zamazor.market.modules.auth.exception.UnauthorizedException;
import com.zamazor.market.modules.auth.models.dto.*;
import com.zamazor.market.modules.user.models.mapper.UserMapper;
import com.zamazor.market.modules.user.models.dto.UserDto;
import com.zamazor.market.modules.user.models.entity.User;
import com.zamazor.market.modules.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final EmailService emailService;
	private final ApplicationProperties application;

	@Transactional
	public UserDto register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyInUseException("An account with this email already in use");
		}
		var user = userMapper.toEntity(request);
		user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
		user.setIsAdmin(false);

		sendRegistrationSuccessEmail(user.getEmail());

		return userMapper.toDto(userRepository.save(user));
	}

	public AuthenticationResult authenticate(AuthenticationRequest request) {
		var authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password())
		);
		if (!(authentication.getPrincipal() instanceof User user)) {
			throw new BadCredentialsException("Invalid Credentials");
		}
		var userDto = userMapper.toDto(user);
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);

		return new AuthenticationResult(refreshToken, accessToken, userDto);
	}

	public TokenPair refreshTokens(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new UnauthorizedException("Refresh token is missing");
		}

		try {
			String email = jwtService.extractRefreshUsername(refreshToken);
			var user = userRepository.findByEmail(email)
					.orElseThrow(() -> new UnauthorizedException("User not found"));

			if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
				throw new UnauthorizedException("Invalid refresh token credentials");
			}

			String newAccessToken = jwtService.generateAccessToken(user);
			String newRefreshToken = jwtService.generateRefreshToken(user);

			return new TokenPair(newAccessToken, newRefreshToken);

		} catch (JwtException | IllegalArgumentException e) {
			throw new UnauthorizedException("Invalid or expired refresh token");
		}
	}

	public UserDto getCurrentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
			throw new UnauthorizedException("User not authenticated");
		}

		return userMapper.toDto(user);
	}

	private void sendRegistrationSuccessEmail(String to) {
		var link = "%s/login".formatted(application.frontendUrl());

		emailService.sendHtmlEmail(
				to,
				"Account Registration Successful!",
				"registration-success",
				Map.of(
						"appName", application.name(),
						"loginUrl", link,
						"supportEmail", application.supportEmail(),
						"supportPhone", application.supportPhone(),
						"year", Year.now().getValue()
				)
		);
	}
}
