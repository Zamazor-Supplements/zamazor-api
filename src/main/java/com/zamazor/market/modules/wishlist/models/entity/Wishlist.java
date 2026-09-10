package com.zamazor.market.modules.wishlist.models.entity;

import com.zamazor.market.modules.product.models.entity.Product;
import com.zamazor.market.modules.user.models.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
		name = "wishlists",
		uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class Wishlist {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id")
	private Product product;

	@CreatedDate
	private Instant createdAt;
}
