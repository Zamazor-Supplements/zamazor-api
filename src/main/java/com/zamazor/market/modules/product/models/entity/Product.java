package com.zamazor.market.modules.product.models.entity;

import com.zamazor.market.modules.catalog.models.entity.CartItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"store", "category"})
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal price;

	@Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
	private String imageUrl;

	@Column(name = "image_public_id", nullable = false)
	private String imagePublicId;

	@Builder.Default
	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity = 0;

	@Builder.Default
	@Column(name = "reserved_quantity", nullable = false)
	private Integer reservedQuantity = 0;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Builder.Default
	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	private List<CartItem> cartItems = new ArrayList<>();

	@Version
	@Builder.Default
	private Long version = 0L;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant modifiedAt;

	public int availableQuantity() {
		return this.stockQuantity - this.reservedQuantity;
	}
}
