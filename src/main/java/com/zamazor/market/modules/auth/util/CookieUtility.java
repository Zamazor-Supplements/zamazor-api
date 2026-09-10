package com.zamazor.market.modules.auth.util;

import com.zamazor.market.security.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtility {

	private final JwtProperties jwtProperties;
	public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(jwtProperties.refresh().expiration().toSeconds())
				.sameSite("None")
				.secure(true) // secure is required to use "SameSite=None"
				.build();
	}
}