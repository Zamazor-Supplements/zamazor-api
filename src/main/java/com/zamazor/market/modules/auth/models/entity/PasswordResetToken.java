package com.zamazor.market.modules.auth.models.entity;

import com.zamazor.market.modules.user.models.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Password-reset tokens (30m TTL) — own dedicated table.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
		@Index(name = "uq_prt_token_hash", columnList = "token_hash", unique = true),
		@Index(name = "idx_prt_user", columnList = "user_id"),
		@Index(name = "idx_prt_expires", columnList = "expires_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends AbstractToken {
	public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
		super(null, user, tokenHash, expiresAt, null, null);
	}
}