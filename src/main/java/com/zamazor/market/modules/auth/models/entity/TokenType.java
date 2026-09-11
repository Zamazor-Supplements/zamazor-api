package com.zamazor.market.modules.auth.models.entity;

public enum TokenType {
	VERIFY_EMAIL,     // 24h TTL
	PASSWORD_RESET,   // 30m TTL
}