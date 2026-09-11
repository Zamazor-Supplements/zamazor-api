package com.zamazor.market.modules.auth.service;

import com.zamazor.market.mail.event.AccountCreatedEvent;
import com.zamazor.market.mail.event.EmailVerificationRequestEvent;
import com.zamazor.market.mail.event.PasswordChangeEvent;
import com.zamazor.market.mail.event.ResetPasswordRequestEvent;
import com.zamazor.market.modules.auth.models.entity.EmailVerificationToken;
import com.zamazor.market.modules.auth.models.entity.PasswordResetToken;
import com.zamazor.market.modules.auth.models.entity.TokenType;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private static final Duration VERIFY_TTL = Duration.ofHours(24);
	private static final Duration RESET_TTL = Duration.ofMinutes(30);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final TokenService tokenService;
	private final ApplicationEventPublisher publisher;
	private final Clock clock;

	@Transactional
	public UserDto register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyInUseException("An account with this email already in use");
		}
		var user = userMapper.toEntity(request);
		user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
		user.setAdmin(false);
		var savedUser = userRepository.save(user);
		var issued = tokenService.issue(savedUser, TokenType.VERIFY_EMAIL, VERIFY_TTL);

		publisher.publishEvent(new AccountCreatedEvent(savedUser.getEmail(), issued.raw()));

		return userMapper.toDto(savedUser);
	}

	@Transactional
	public void sendVerificationEmail(String email) {
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null || user.isEmailVerified()) {
			return; // same response whether the user exists
		}
		var issued = tokenService.issue(user, TokenType.VERIFY_EMAIL, VERIFY_TTL);
		publisher.publishEvent(new EmailVerificationRequestEvent(user.getEmail(), issued.raw()));
	}

	@Transactional
	public void verifyEmail(VerifyEmailRequest request) {
		EmailVerificationToken token =
				(EmailVerificationToken) tokenService.consume(request.token(), TokenType.VERIFY_EMAIL);
		token.getUser().markEmailVerified(Instant.now(clock));
		token.markUsed(Instant.now(clock));
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

	@Transactional
	public void requestPasswordReset(PasswordResetRequest request) {
		userRepository.findByEmail(request.email()).ifPresent(user -> {
			var issued = tokenService.issue(user, TokenType.PASSWORD_RESET, RESET_TTL);
			publisher.publishEvent(new ResetPasswordRequestEvent(user.getEmail(), issued.raw()));
		});
	}

	@Transactional
	public void resetPassword(PasswordResetConfirmationRequest request) {
		PasswordResetToken token =
				(PasswordResetToken) tokenService.consume(request.token(), TokenType.PASSWORD_RESET);
		var user = token.getUser();
		user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.newPassword())));
		token.markUsed(Instant.now(clock));
		publisher.publishEvent(new PasswordChangeEvent(user.getEmail()));
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
}
