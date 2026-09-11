package com.zamazor.market.mail.event;

public record AccountCreatedEvent(String to, String token) {
}