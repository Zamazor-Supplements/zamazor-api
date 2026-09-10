package com.zamazor.market.modules.catalog.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
		name = "order_items",
		uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id"})
)
public class OrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "product_id")
	private UUID productId;

	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(name = "product_image_url", columnDefinition = "TEXT")
	private String productImageUrl;

	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private Integer quantity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
}