package com.zamazor.market.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.modules.catalog.models.entity.Order;
import com.zamazor.market.modules.catalog.repository.OrderRepository;
import com.zamazor.market.payment.exception.PaymentGatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
	private final ApplicationProperties application;
	private final OrderRepository orderRepository;
	private final Clock clock;

	public Session createCheckoutSession(Order order) {
		String orderId = order.getId().toString();
		String oldSessionId = order.getStripeCheckoutSessionId();
		String userId = order.getUser().getId().toString();

		String successUrl = UriComponentsBuilder
				.fromUriString(application.frontendUrl())
				.pathSegment("checkout", "orders", orderId, "success")
				.queryParam("sessionId", "{CHECKOUT_SESSION_ID}")
				.build(false)
				.toUriString();

		String cancelUrl = UriComponentsBuilder
				.fromUriString(application.frontendUrl())
				.pathSegment("checkout", "orders", orderId, "cancel")
				.build()
				.toUriString();

		try {
			SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
					.setMode(SessionCreateParams.Mode.PAYMENT)
					.setSuccessUrl(successUrl)
					.setCancelUrl(cancelUrl)
					.setCustomerEmail(order.getUser().getEmail())
					.setPhoneNumberCollection(
							SessionCreateParams.PhoneNumberCollection.builder()
									.setEnabled(true)
									.build()
					)
					.setClientReferenceId(orderId)
					.putMetadata("order_id", orderId)
					.putMetadata("user_id", userId)
					.setPaymentIntentData(
							SessionCreateParams.PaymentIntentData.builder()
									.putMetadata("order_id", orderId)
									.putMetadata("user_id", userId)
									.build()
					)
					.setShippingAddressCollection(
							SessionCreateParams.ShippingAddressCollection.builder()
									.addAllowedCountry(SessionCreateParams.ShippingAddressCollection.AllowedCountry.MA)
									.build()
					)
					.setAutomaticTax(
							SessionCreateParams.AutomaticTax.builder()
									.setEnabled(true)
									.build()
					)
					.setExpiresAt(Instant.now(clock).plus(30, ChronoUnit.MINUTES).getEpochSecond());
//			Planned to be implemented later; causes different totals between stripe and application DB
			// Inline shipping options — zero extra network calls to Stripe
//					.addShippingOption(
//							SessionCreateParams.ShippingOption.builder()
//									.setShippingRateData(
//											SessionCreateParams.ShippingOption.ShippingRateData.builder()
//													.setDisplayName("Standard Delivery")
//													.setType(SessionCreateParams.ShippingOption.ShippingRateData.Type.FIXED_AMOUNT)
//													.setFixedAmount(
//															SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount.builder()
//																	.setAmount(3000L)
//																	.setCurrency("mad")
//																	.build()
//													)
//													.build()
//									)
//									.build()
//					)
//					.addShippingOption(
//							SessionCreateParams.ShippingOption.builder()
//									.setShippingRateData(
//											SessionCreateParams.ShippingOption.ShippingRateData.builder()
//													.setDisplayName("Express Delivery")
//													.setType(SessionCreateParams.ShippingOption.ShippingRateData.Type.FIXED_AMOUNT)
//													.setFixedAmount(
//															SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount.builder()
//																	.setAmount(6000L)
//																	.setCurrency("mad")
//																	.build()
//													)
//													.build()
//									)
//									.build()
//					);

			// Populate line items with safe BigDecimal rounding
			order.getItems().forEach(item -> {
				long unitAmount = item.getUnitPrice()
						.setScale(2, RoundingMode.HALF_UP)
						.movePointRight(2)
						.longValue();

				SessionCreateParams.LineItem.PriceData.ProductData.Builder productBuilder =
						SessionCreateParams.LineItem.PriceData.ProductData.builder()
								.setName(item.getProductName());

				if (item.getProductImageUrl() != null && !item.getProductImageUrl().isBlank()) {
					productBuilder.addImage(item.getProductImageUrl());
				}

				sessionBuilder.addLineItem(
						SessionCreateParams.LineItem.builder()
								.setQuantity((long) item.getQuantity())
								.setPriceData(
										SessionCreateParams.LineItem.PriceData.builder()
												.setCurrency("mad")
												.setUnitAmount(unitAmount)
												.setProductData(productBuilder.build())
												.build()
								)
								.build()
				);
			});

			String idempotencyKey = "order-%s-attempt-%d".formatted(orderId, order.getPaymentAttemptCount());

			RequestOptions requestOptions = RequestOptions.builder()
					.setIdempotencyKey(idempotencyKey)
					.build();

			Session session = Session.create(sessionBuilder.build(), requestOptions);

			// 2. EXPIRE THE OLD SESSION ON STRIPE IF IT EXISTS
			if (oldSessionId != null && !oldSessionId.equals(session.getId())) {
				try {
					Session oldSession = Session.retrieve(oldSessionId);
					if ("open".equals(oldSession.getStatus())) {
						oldSession.expire();
						log.debug("Successfully expired old Stripe checkout session {} for order {}", oldSessionId, orderId);
					}
				} catch (StripeException e) {
					log.warn("Could not expire previous Stripe session {}: {}", oldSessionId, e.getMessage());
				}
			}

			order.setStripeCheckoutSessionId(session.getId());
			if (session.getPaymentIntent() != null) {
				order.setStripePaymentIntentId(session.getPaymentIntent());
			}
			orderRepository.save(order);

			log.info("Successfully created Stripe Checkout Session {} for order {}", session.getId(), orderId);
			return session;
		} catch (StripeException e) {
			log.error("Stripe API communication failure for order {}. Error: {}", orderId, e.getMessage(), e);
			throw new PaymentGatewayException("Could not initiate payment gateway session. Please try again later.");
		}
	}

	public void refundPayment(String stripePaymentIntentId, BigDecimal amount) {
		try {
			// If amount is null or full refund, pass the payment intent ID directly
			RefundCreateParams params = RefundCreateParams.builder()
					.setPaymentIntent(stripePaymentIntentId)
					// If you want partial refunds, convert BigDecimal to cents:
					.setAmount(amount.movePointRight(2).longValue())
					.build();

			Refund.create(params);
		} catch (StripeException e) {
			throw new PaymentGatewayException("Failed to process refund with Stripe: " + e.getMessage());
		}
	}

	public Session expireCheckoutSession(String sessionId) {
		try {
			Session session = Session.retrieve(sessionId);
			return session.expire();
		} catch (StripeException e) {
			throw new RuntimeException("Failed to expire Stripe checkout session: " + e.getMessage(), e);
		}
	}

	public boolean isSessionUnPaid(String stripeSessionId) {
		try {
			Session session = Session.retrieve(stripeSessionId);
			return !"complete".equals(session.getStatus()) || !"paid".equals(session.getPaymentStatus());

		} catch (StripeException e) {
			log.error("Failed to verify session status with Stripe API for Session ID: {}", stripeSessionId, e);
			throw new PaymentGatewayException("Could not verify payment with processor. Please try again.");
		}
	}
}
