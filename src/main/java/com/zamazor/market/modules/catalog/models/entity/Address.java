package com.zamazor.market.modules.catalog.models.entity;

import com.zamazor.market.modules.user.models.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(mappedBy = "address")
	private User user;

	@Embedded
	private AddressComponent addressDetails;
}