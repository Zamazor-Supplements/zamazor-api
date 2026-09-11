package com.zamazor.market.security.crypto;

import com.zamazor.market.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
	private final JwtProperties jwtProperties;
	private final JwtKeyProvider keyProvider;
	private final Clock clock;

	public String extractAccessUsername(String token) {
		return extractUsername(token, keyProvider.getAccessKey());
	}

	public String extractRefreshUsername(String token) {
		return extractUsername(token, keyProvider.getRefreshKey());
	}

	public boolean isAccessTokenValid(String token, UserDetails user) {
		return isTokenValid(token, user, keyProvider.getAccessKey());
	}

	public boolean isRefreshTokenValid(String token, UserDetails user) {
		return isTokenValid(token, user, keyProvider.getRefreshKey());
	}

	public String generateAccessToken(UserDetails userDetails) {
		return generateAccessToken(new HashMap<>(), userDetails);
	}

	public String generateAccessToken(Map<String, Object> extraClaims, UserDetails user) {
		return buildToken(extraClaims, user, jwtProperties.access().expiration(), keyProvider.getAccessKey());
	}

	public String generateRefreshToken(UserDetails user) {
		return buildToken(new HashMap<>(), user, jwtProperties.refresh().expiration(), keyProvider.getRefreshKey());
	}

	public @NotNull Duration getAccessTokenExpirationTime() {
		return jwtProperties.access().expiration();
	}

	public @NotNull Duration getRefreshTokenExpirationTime() {
		return jwtProperties.refresh().expiration();
	}

	private String extractUsername(String token, SecretKey alignmentKey) {
		return extractClaim(token, alignmentKey, Claims::getSubject);
	}

	private <T> T extractClaim(String token, SecretKey alignmentKey, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token, alignmentKey);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token, SecretKey alignmentKey) {
		return Jwts.parser()
				.verifyWith(alignmentKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails user, Duration duration, SecretKey signingKey) {
		Instant now = Instant.now(clock);
		Instant expiryLimit = now.plus(duration);

		return Jwts.builder()
				.claims(extraClaims)
				.subject(user.getUsername())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiryLimit))
				.signWith(signingKey)
				.issuer(jwtProperties.issuer())
				.compact();
	}

	private boolean isTokenValid(String token, UserDetails user, SecretKey alignmentKey) {
		final String username = extractUsername(token, alignmentKey);
		return (username.equals(user.getUsername())) && !isTokenExpired(token, alignmentKey);
	}

	private boolean isTokenExpired(String token, SecretKey alignmentKey) {
		Date expiration = extractClaim(token, alignmentKey, Claims::getExpiration);
		return expiration.before(new Date());
	}
}
