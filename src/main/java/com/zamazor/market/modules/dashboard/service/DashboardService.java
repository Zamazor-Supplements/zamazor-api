package com.zamazor.market.modules.dashboard.service;

import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.models.entity.OrderItem;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.modules.dashboard.models.dto.*;
import com.zamazor.market.modules.dashboard.models.mapper.DashboardMapper;
import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.product.repository.CategoryRepository;
import com.zamazor.market.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final DashboardMapper dashboardMapper;
	private static final int LOW_STOCK_THRESHOLD = 10;

	private static final Set<OrderStatus> SETTLED_STATUSES = Set.of(
			OrderStatus.CONFIRMED,
			OrderStatus.SHIPPED,
			OrderStatus.DELIVERED
	);
	private final CategoryRepository categoryRepository;

	public DashboardOverviewDto getDashboardOverview() {
		List<Order> allOrders = orderRepository.findAll();
		List<Product> allProducts = productRepository.findAll();

		List<Order> settledOrders = allOrders.stream()
				.filter(o -> SETTLED_STATUSES.contains(o.getStatus()))
				.toList();

		BigDecimal totalSales = settledOrders.stream()
				.map(Order::getTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal averageOrderValue = settledOrders.isEmpty()
				? BigDecimal.ZERO
				: totalSales.divide(BigDecimal.valueOf(settledOrders.size()), 2, RoundingMode.HALF_UP);

		long totalOrders = allOrders.size();
		long pending = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
		long completed = settledOrders.size();
		long canceled = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELED).count();
		long inFlight = pending + completed;

		List<RecentOrderDto> recentOrders = allOrders.stream()
				.sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(6)
				.map(dashboardMapper::toRecentOrderDto)
				.toList();

		List<LowStockProductDto> lowStockProducts = allProducts.stream()
				.filter(p -> p.getStockQuantity() <= LOW_STOCK_THRESHOLD)
				.sorted(Comparator.comparing(Product::getStockQuantity))
				.map(dashboardMapper::toLowStockProductDto)
				.toList();

		List<CategorySummaryDto> categorySummary = allProducts.stream()
				.collect(Collectors.groupingBy(
						p -> p.getCategory() != null ? p.getCategory().getLabel() : "Uncategorized",
						Collectors.counting()
				))
				.entrySet().stream()
				.map(e -> new CategorySummaryDto(e.getKey(), e.getValue()))
				.sorted(Comparator.comparing(CategorySummaryDto::count).reversed())
				.limit(5)
				.toList();

		List<TopProductDto> topProducts = aggregateTopProducts(settledOrders, allProducts);

		return new DashboardOverviewDto(
				totalSales,
				averageOrderValue,
				totalOrders,
				pending,
				completed,
				canceled,
				inFlight,
				recentOrders,
				lowStockProducts,
				categorySummary,
				topProducts
		);
	}

	public List<CategoryAnalyticsDto> getCategoriesWithCounts() {
		var projections = categoryRepository.findAllWithProductCounts();
		return dashboardMapper.toAnalyticsDtoList(projections);
	}

	public ProductAnalyticsDto getProductAnalytics() {
		ProductRepository.MetricsSummary summary = productRepository.getMetricsSummary();

		return new ProductAnalyticsDto(
				summary.getTotalProducts(),
				summary.getTotalCategories(),
				productRepository.countLowStockProducts(LOW_STOCK_THRESHOLD),
				BigDecimal.valueOf(summary.getAveragePrice())
						.setScale(2, RoundingMode.HALF_UP)
		);
	}

	private List<TopProductDto> aggregateTopProducts(List<Order> settledOrders, List<Product> products) {
		Map<UUID, String> productCategoryMap = products.stream()
				.collect(Collectors.toMap(
						Product::getId,
						p -> p.getCategory() != null ? p.getCategory().getLabel() : "Uncategorized",
						(existing, replacement) -> existing
				));

		Map<UUID, TopProductDto> aggregateMap = new HashMap<>();

		for (Order order : settledOrders) {
			for (OrderItem item : order.getItems()) {
				UUID pId = item.getProductId();
				if (pId == null) continue;

				int qty = Optional.ofNullable(item.getQuantity()).orElse(0);
				BigDecimal unitPrice = Optional.ofNullable(item.getUnitPrice()).orElse(BigDecimal.ZERO);
				BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(qty));
				String category = productCategoryMap.getOrDefault(pId, "Uncategorized");

				aggregateMap.merge(pId,
						new TopProductDto(pId, item.getProductName(), qty, revenue, category),
						(oldVal, newVal) -> new TopProductDto(
								pId,
								oldVal.name(),
								oldVal.quantity() + newVal.quantity(),
								oldVal.revenue().add(newVal.revenue()),
								category
						)
				);
			}
		}

		return aggregateMap.values().stream()
				.sorted(Comparator.comparing(TopProductDto::quantity).reversed())
				.limit(5)
				.toList();
	}
}