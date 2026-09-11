package com.zamazor.market.modules.dashboard.controller;

import com.zamazor.market.modules.dashboard.models.dto.CategoryMetrics;
import com.zamazor.market.modules.dashboard.models.dto.OverviewMetrics;
import com.zamazor.market.modules.dashboard.models.dto.ProductMetrics;
import com.zamazor.market.modules.dashboard.service.AdminSseService;
import com.zamazor.market.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
	private final DashboardService dashboardService;
	private final AdminSseService adminSseService;

	@GetMapping("/overview")
	public ResponseEntity<OverviewMetrics> getOverview() {
		return ResponseEntity.ok(dashboardService.getDashboardOverview());
	}

	@GetMapping("/category")
	public ResponseEntity<List<CategoryMetrics>> getCategoryAnalytics() {
		return ResponseEntity.ok(dashboardService.getCategoriesWithCounts());
	}

	@GetMapping("/product")
	public ResponseEntity<ProductMetrics> getProductAnalytics() {
		return ResponseEntity.ok(dashboardService.getProductAnalytics());
	}

	@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter events() {
		return adminSseService.connect();
	}
}