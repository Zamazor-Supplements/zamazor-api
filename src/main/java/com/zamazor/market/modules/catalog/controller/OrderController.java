package com.zamazor.market.modules.catalog.controller;

import com.zamazor.market.modules.billing.models.dto.CheckoutRequest;
import com.zamazor.market.modules.billing.models.dto.PaymentSessionResponse;
import com.zamazor.market.modules.billing.service.OrderPaymentService;
import com.zamazor.market.modules.catalog.models.dto.UpdateStatusRequest;
import com.zamazor.market.shared.api.PageResponse;
import com.zamazor.market.modules.catalog.models.dto.OrderDto;
import com.zamazor.market.modules.catalog.models.entity.OrderStatus;
import com.zamazor.market.modules.catalog.service.OrderService;
import com.zamazor.market.modules.user.models.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;
	private final OrderPaymentService orderPaymentService;

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ResponseEntity<PageResponse<OrderDto>> getAllOrders(
			@RequestParam(required = false) String userFullName,
			@RequestParam(required = false) @Valid OrderStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt,desc") String[] sort
	) {
		String sortProperty = sort[0];
		Sort.Direction direction = Sort.Direction.DESC;

		if (sort.length > 1) {
			direction = Sort.Direction.fromString(sort[1]);
		}

		if ("totalAmount".equals(sortProperty)) {
			sortProperty = "total";
		} else if ("userFullName".equals(sortProperty)) {
			sortProperty = "user.fullName";
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
		return ResponseEntity.ok(orderService.getAll(userFullName, status, pageable));
	}

	@GetMapping("/me")
	public ResponseEntity<PageResponse<OrderDto>> getMyOrders(
			@AuthenticationPrincipal User user,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "10") Integer size
	) {
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(orderService.getByUserId(user.getId(), pageable));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderDto> getOrderById(@PathVariable UUID orderId) {
		OrderDto order = orderService.getById(orderId);
		return ResponseEntity.ok(order);
	}

	@PostMapping("/checkout")
	public ResponseEntity<OrderDto> checkout(@AuthenticationPrincipal User user, @Valid @RequestBody CheckoutRequest request) {
		return ResponseEntity.ok(orderPaymentService.checkout(user, request));
	}

	@GetMapping("/checkout/{orderId}/pay")
	public ResponseEntity<PaymentSessionResponse> payOrder(@PathVariable UUID orderId) {
		return ResponseEntity.ok(orderPaymentService.regeneratePaymentLink(orderId));
	}

	@PostMapping("/checkout/{orderId}/verify")
	public ResponseEntity<OrderDto> verifyPaymentStatus(
			@PathVariable UUID orderId,
			@RequestParam("sessionId") String sessionId,
			@AuthenticationPrincipal User user
	) {
		OrderDto verifiedOrder = orderService.verifyOrderPayment(orderId, sessionId, user);
		return ResponseEntity.ok(verifiedOrder);
	}

	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<OrderDto> cancelOrder(@PathVariable UUID orderId) {
		return ResponseEntity.ok(orderService.cancelOrder(orderId));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderDto> changeStatus(@PathVariable UUID orderId, @Valid @RequestBody UpdateStatusRequest request) {
		return ResponseEntity.ok(orderService.changeStatus(orderId, request.status()));
	}
}
