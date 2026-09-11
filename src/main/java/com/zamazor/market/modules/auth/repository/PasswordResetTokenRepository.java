package com.zamazor.market.modules.auth.repository;

import com.zamazor.market.modules.auth.models.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
	@Modifying
	@Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId AND t.usedAt IS NULL")
	void deleteUnusedForUser(@Param("userId") UUID userId);

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}