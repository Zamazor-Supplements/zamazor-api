package com.zamazor.market.modules.auth.repository;

import com.zamazor.market.modules.auth.models.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
	@Modifying
	@Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId AND t.usedAt IS NULL")
	void deleteUnusedForUser(@Param("userId") UUID userId);

	Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}