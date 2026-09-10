package com.zamazor.market.modules.auth.controller;

import com.zamazor.market.modules.auth.models.dto.*;
import com.zamazor.market.modules.auth.models.mapper.AuthenticationMapper;
import com.zamazor.market.modules.auth.service.AuthenticationService;
import com.zamazor.market.modules.auth.util.CookieUtility;
import com.zamazor.market.modules.user.models.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService authenticationService;
	private final CookieUtility cookieUtility;
	private final AuthenticationMapper authenticationMapper;

	@PostMapping("/register")
	public ResponseEntity<UserDto> register(@RequestBody @Valid RegisterRequest request, UriComponentsBuilder uriBuilder) {
		UserDto response = authenticationService.register(request);
		var uri = uriBuilder.path("/users/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(uri).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request, HttpServletResponse response) {
		AuthenticationResult authenticationResult = authenticationService.authenticate(request);
		ResponseCookie cookie = cookieUtility.createRefreshTokenCookie(authenticationResult.refreshToken());

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return ResponseEntity.ok(authenticationMapper.toAuthenticationResponse(authenticationResult));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refresh(
			@CookieValue(name = CookieUtility.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
			HttpServletResponse response
	) {
		TokenPair tokenPair = authenticationService.refreshTokens(refreshToken);
		ResponseCookie cookie = cookieUtility.createRefreshTokenCookie(tokenPair.refreshToken());

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return ResponseEntity.ok(authenticationMapper.toRefreshResponse(tokenPair));
	}

	@GetMapping("/me")
	public ResponseEntity<UserDto> me() {
		return ResponseEntity.ok(authenticationService.getCurrentUser());
	}
}
