package com.zamazor.market.modules.auth.models.entity;

import com.zamazor.market.modules.user.models.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared, non-persisted base for both token entities. Each concrete token type
 * maps to its OWN table (see the subclasses) — no polymorphic discriminator.
 * <p>
 * Lombok: {@code @Getter} only — never {@code @Data}/{@code @EqualsAndHashCode}
 * on JPA entities (they would drag lazy associations into equals/hashCode).
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * SHA-256 of the raw token. The raw value is never persisted.
	 */
	@Column(name = "token_hash", length = 64, nullable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AbstractToken(UUID id, User user, String tokenHash, Instant expiresAt) {
		this.id = id;
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isValid(Instant now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

	public void markUsed(Instant now) {
		this.usedAt = now;
	}
}