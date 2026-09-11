package com.zamazor.market.modules.auth.service;

import com.zamazor.market.modules.auth.exception.TokenInvalidException;
import com.zamazor.market.modules.auth.models.entity.AbstractToken;
import com.zamazor.market.modules.auth.models.entity.EmailVerificationToken;
import com.zamazor.market.modules.auth.models.entity.PasswordResetToken;
import com.zamazor.market.modules.auth.models.entity.TokenType;
import com.zamazor.market.modules.auth.repository.EmailVerificationTokenRepository;
import com.zamazor.market.modules.auth.repository.PasswordResetTokenRepository;
import com.zamazor.market.modules.user.models.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and consumes tokens across dedicated token tables.
 * <p>
 * Tokens are opaque random values stored server-side as SHA-256 hashes.
 */
@Service
@RequiredArgsConstructor
public class TokenService {
	private static final int TOKEN_BYTES = 32; // 256 bits of entropy
	private static final SecureRandom RANDOM = new SecureRandom();

	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final Clock clock;

	/**
	 * Issues a token into the table matching {@code type}.
	 * Rotates out any existing unused tokens for the user in that flow.
	 */
	@Transactional
	public IssuedToken issue(User user, TokenType type, Duration ttl) {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String hash = hashToken(raw);
		Instant expiresAt = Instant.now(clock).plus(ttl);

		AbstractToken entity = switch (type) {
			case VERIFY_EMAIL -> {
				emailVerificationTokenRepository.deleteUnusedForUser(user.getId());
				yield emailVerificationTokenRepository.save(
						new EmailVerificationToken(user, hash, expiresAt));
			}
			case PASSWORD_RESET -> {
				passwordResetTokenRepository.deleteUnusedForUser(user.getId());
				yield passwordResetTokenRepository.save(
						new PasswordResetToken(user, hash, expiresAt));
			}
		};
		return new IssuedToken(raw, entity);
	}

	/**
	 * Consumes a token from the table matching {@code type}.
	 */
	@Transactional
	public AbstractToken consume(String raw, TokenType type) {
		String hash = hashToken(raw);

		AbstractToken token = switch (type) {
			case VERIFY_EMAIL -> emailVerificationTokenRepository.findByTokenHash(hash)
					.orElseThrow(TokenInvalidException::new);
			case PASSWORD_RESET -> passwordResetTokenRepository.findByTokenHash(hash)
					.orElseThrow(TokenInvalidException::new);
		};

		if (!token.isValid(Instant.now(clock))) {
			throw new TokenInvalidException();
		}

		token.markUsed(Instant.now(clock));
		return token;
	}

	private static String hashToken(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available on JVM", e);
		}
	}

	public record IssuedToken(String raw, AbstractToken entity) {
	}
}