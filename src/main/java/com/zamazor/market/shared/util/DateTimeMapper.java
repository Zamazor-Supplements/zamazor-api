package com.zamazor.market.shared.util;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DateTimeMapper {
	public Instant mapSecondsToInstant(Long seconds) {
		return seconds == null ? null : Instant.ofEpochSecond(seconds);
	}
}