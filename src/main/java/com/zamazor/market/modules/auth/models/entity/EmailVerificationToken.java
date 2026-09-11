package com.zamazor.market.modules.auth.models.entity;

import com.zamazor.market.modules.user.models.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens", indexes = {
		@Index(name = "uq_evt_token_hash", columnList = "token_hash", unique = true),
		@Index(name = "idx_evt_user", columnList = "user_id"),
		@Index(name = "idx_evt_expires", columnList = "expires_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken extends AbstractToken {
	public EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
		super(null, user, tokenHash, expiresAt, null, null);
	}
}