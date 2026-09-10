package com.zamazor.market.modules.user.models.entity;

import com.zamazor.market.modules.catalog.models.entity.Address;
import com.zamazor.market.modules.catalog.models.entity.Cart;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.wishlist.models.entity.Wishlist;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@NullMarked
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements UserDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "full_name")
	private String fullName;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(name = "is_admin", nullable = false)
	private Boolean isAdmin;

	@Column(nullable = false)
	private String password;

	public Role getRole() {
		return isAdmin ? Role.ADMIN : Role.USER;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(getRole());
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Nullable
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id")
	private Address address;

	@Nullable
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
	private List<Order> orders;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
	private List<Wishlist> wishlists;

	public void setCart(Cart cart) {
		this.cart = cart;

		if (cart.getUser() != null) {
			cart.setUser(this);
		}
	}

	public void setAddress(Address address) {
		this.address = address;

		if (address.getUser() != null) {
			address.setUser(this);
		}
	}
}
