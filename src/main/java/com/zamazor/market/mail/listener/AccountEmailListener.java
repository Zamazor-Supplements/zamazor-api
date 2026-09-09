package com.zamazor.market.mail.listener;

import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.mail.event.*;
import com.zamazor.market.mail.factory.EmailContextFactory;
import com.zamazor.market.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccountEmailListener {
	private final EmailService emailService;
	private final ApplicationProperties application;
	private final EmailContextFactory emailContextFactory;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleAccountCreatedNotification(AccountCreatedEvent event) {
		var verificationToken = event.token();
		var to = event.to();
		var loginLink = "%s/login".formatted(application.frontendUrl());
		var verificationLink = "%s/verify-email?token=%s".formatted(application.frontendUrl(), verificationToken);

		Map<String, Object> variables = emailContextFactory.create();
		variables.put("verificationLink", verificationLink);
		variables.put("loginLink", loginLink);

		emailService.sendHtmlEmail(
				to,
				"Account Registration Successful!",
				"registration-success",
				variables
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleEmailVerificationNotification(EmailVerificationRequestEvent event) {
		var verificationToken = event.token();
		var to = event.to();
		var verificationLink = "%s/verify-email?token=%s".formatted(application.frontendUrl(), verificationToken);

		Map<String, Object> variables = emailContextFactory.create();
		variables.put("verificationLink", verificationLink);

		emailService.sendHtmlEmail(
				to,
				"Verify Your Email",
				"email-verification",
				variables
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleResetPasswordRequestNotification(ResetPasswordRequestEvent event) {
		String resetToken = event.token();
		String to = event.to();
		String resetLink = "%s/reset-password?token=%s".formatted(application.frontendUrl(), resetToken);

		Map<String, Object> variables = emailContextFactory.create();
		variables.put("resetLink", resetLink);

		emailService.sendHtmlEmail(
				to,
				"Reset Your Password",
				"password-reset",
				variables
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePasswordChange(PasswordChangeEvent event) {
		String to = event.to();
		Map<String, Object> variables = emailContextFactory.create();

		emailService.sendHtmlEmail(to, "Your Password Has Been Changed",
				"password-changed",
				variables
		);
	}
}
