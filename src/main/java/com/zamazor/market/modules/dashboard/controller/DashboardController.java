package com.zamazor.market.modules.dashboard.controller;

import com.zamazor.market.modules.dashboard.models.dto.CategoryAnalyticsDto;
import com.zamazor.market.modules.dashboard.models.dto.DashboardOverviewDto;
import com.zamazor.market.modules.dashboard.models.dto.ProductAnalyticsDto;
import com.zamazor.market.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
	private final DashboardService dashboardService;

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/overview")
	public ResponseEntity<DashboardOverviewDto> getOverview() {
		return ResponseEntity.ok(dashboardService.getDashboardOverview());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/category")
	public ResponseEntity<List<CategoryAnalyticsDto>> getCategoryAnalytics() {
		return ResponseEntity.ok(dashboardService.getCategoriesWithCounts());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/product")
	public ResponseEntity<ProductAnalyticsDto> getProductAnalytics() {
		return ResponseEntity.ok(dashboardService.getProductAnalytics());
	}
}