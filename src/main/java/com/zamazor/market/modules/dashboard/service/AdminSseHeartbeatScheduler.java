package com.zamazor.market.modules.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSseHeartbeatScheduler {
	private final AdminSseService adminSseService;

	@Scheduled(fixedRate = 20_000)
	public void heartbeat() {
		adminSseService.heartbeat();
	}
}
