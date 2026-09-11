package com.zamazor.market.mail.event;

public record ResetPasswordRequestEvent(String to, String token) {
}