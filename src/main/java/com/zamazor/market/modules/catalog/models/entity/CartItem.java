package com.zamazor.market.modules.catalog.models.entity;

import com.zamazor.market.modules.product.models.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
		name = "cart_items",
		uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"})
)
public class CartItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private Integer quantity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	public BigDecimal getLineTotal() {
		return product.getPrice().multiply(BigDecimal.valueOf(quantity));
	}
}