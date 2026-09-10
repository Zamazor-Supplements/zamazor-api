package com.zamazor.market.shared.exception;

import com.zamazor.market.media.exception.MediaStorageException;
import com.zamazor.market.modules.catalog.exception.IllegalOrderTransitionException;
import com.zamazor.market.modules.auth.exception.EmailAlreadyInUseException;
import com.zamazor.market.modules.auth.exception.UnauthorizedException;
import com.zamazor.market.modules.catalog.exception.*;
import com.zamazor.market.modules.product.exception.CategoryNotFoundException;
import com.zamazor.market.modules.product.exception.OrderCancellationException;
import com.zamazor.market.modules.product.exception.OrderRefundException;
import com.zamazor.market.modules.product.exception.ProductNotFoundException;
import com.zamazor.market.modules.product.exception.CategoryAlreadyExistsException;
import com.zamazor.market.modules.user.exception.UserNotFoundException;
import com.zamazor.market.payment.exception.PaymentGatewayException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.NonNull;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ExceptionResolver extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleSecurityException(Exception exception) {
		return switch (exception) {
			case BadCredentialsException e -> createProblemDetail(401, e.getMessage(), "Invalid Credentials");
			case AccountStatusException e -> createProblemDetail(403, e.getMessage(), "Account Locked");
			case UsernameNotFoundException e -> createProblemDetail(401, e.getMessage(), "User Not Found");
			case AccessDeniedException e -> createProblemDetail(403, e.getMessage(), "Unauthorized to access this resource");
			case SignatureException e -> createProblemDetail(403, e.getMessage(), "Invalid JWT Signature");
			case ExpiredJwtException e ->
					createProblemDetail(401, e.getMessage(), "JWT token has expired"); // should be 401 to allow refresh in the frontend
			case ObjectOptimisticLockingFailureException e ->
					createProblemDetail(409, e.getMessage(), "Optimistic locking failed");
			case EmailAlreadyInUseException e -> createProblemDetail(409, e.getMessage(), "Email already in use");
			case UnauthorizedException e -> createProblemDetail(401, e.getMessage(), "Authentication Required");
			case InsufficientAuthenticationException e -> createProblemDetail(401, e.getMessage(), "Authentication Required");
			case UserNotFoundException e -> createProblemDetail(404, e.getMessage(), "User Not Found");
			case ProductNotFoundException e -> createProblemDetail(404, e.getMessage(), "Product Not Found");
			case CategoryNotFoundException e -> createProblemDetail(404, e.getMessage(), "Category Not Found");
			case CategoryAlreadyExistsException e -> createProblemDetail(409, e.getMessage(), "Category Already Exists");
			case MediaStorageException e ->
					createProblemDetail(500, e.getMessage(), "Media storage service failed to process asset");
			case CartNotFoundException e -> createProblemDetail(404, e.getMessage(), "Cart Not Found");
			case OrderNotFoundException e -> createProblemDetail(404, e.getMessage(), "Order Not Found");
			case IllegalOrderTransitionException e ->
					createProblemDetail(409, e.getMessage(), "Order status cannot be changed");
			case OrderCancellationException e -> createProblemDetail(409, e.getMessage(), "Order Cancellation Failed");
			case OrderRefundException e -> createProblemDetail(409, e.getMessage(), "Order Refund Failed");
			case EmptyCartException e -> createProblemDetail(400, e.getMessage(), "Cart is Empty");
			case CartItemNotFoundException e -> createProblemDetail(404, e.getMessage(), "Cart Item Not Found");
			case OutOfStockException e -> createProblemDetail(409, e.getMessage(), "Insufficient Product Stock");
			case AddressNotFoundException e -> createProblemDetail(404, e.getMessage(), "Address Not Found");
			case UnauthorizedOrderException e -> createProblemDetail(403, e.getMessage(), "Unauthorized To Modify Order");
			case PaymentGatewayException e -> createProblemDetail(502, e.getMessage(), "Payment Gateway Error");
			default -> createProblemDetail(500, exception.getMessage(), "Unknown internal server error");
		};
	}

	private ProblemDetail createProblemDetail(int status, String message, String description) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
		detail.setProperty("description", description);
		return detail;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			@NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status,
			@NonNull WebRequest request
	) {
		Map<String, List<String>> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.groupingBy(
						FieldError::getField,
						Collectors.mapping(
								DefaultMessageSourceResolvable::getDefaultMessage,
								Collectors.toList()
						)
				));

		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				status,
				"One or more fields failed validation checks."
		);
		body.setTitle("Validation Failed");
		body.setProperty("description", "One or more fields failed validation checks.");
		body.setProperty("fields", fieldErrors);

		return createResponseEntity(body, headers, status, request);
	}
}