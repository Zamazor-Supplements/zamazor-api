package com.zamazor.market.mail.event;

public record EmailVerificationRequestEvent(String to, String token) {
}