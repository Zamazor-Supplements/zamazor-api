package com.zamazor.market.mail.listener;

import com.zamazor.market.config.ApplicationProperties;
import com.zamazor.market.mail.event.*;
import com.zamazor.market.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Year;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccountEmailListener {
	private final EmailService emailService;
	private final ApplicationProperties application;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleAccountCreatedNotification(AccountCreatedEvent event) {
		var verificationToken = event.token();
		var to = event.to();
		var loginLink = "%s/login".formatted(application.frontendUrl());
		var verificationLink = "%s/verify-email?token=%s".formatted(application.frontendUrl(), verificationToken);

		emailService.sendHtmlEmail(
				to,
				"Account Registration Successful!",
				"registration-success",
				Map.of(
						"appName", application.name(),
						"verificationUrl", verificationLink,
						"loginUrl", loginLink,
						"supportEmail", application.supportEmail(),
						"supportPhone", application.supportPhone(),
						"year", Year.now().getValue()
				)
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleEmailVerificationNotification(EmailVerificationRequestEvent event) {
		var verificationToken = event.token();
		var to = event.to();
		var verificationLink = "%s/verify-email?token=%s".formatted(application.frontendUrl(), verificationToken);

		emailService.sendHtmlEmail(
				to,
				"Verify Your Email",
				"email-verification",
				Map.of(
						"appName", application.name(),
						"verificationUrl", verificationLink,
						"supportEmail", application.supportEmail(),
						"supportPhone", application.supportPhone(),
						"year", Year.now().getValue()
				)
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleResetPasswordRequestNotification(ResetPasswordRequestEvent event) {
		String resetToken = event.token();
		String to = event.to();

		String resetLink = "%s/reset-password?token=%s".formatted(application.frontendUrl(), resetToken);

		emailService.sendHtmlEmail(
				to,
				"Reset Your Password",
				"password-reset",
				Map.of(
						"appName", application.name(),
						"resetUrl", resetLink,
						"supportEmail", application.supportEmail(),
						"supportPhone", application.supportPhone(),
						"year", Year.now().getValue()
				)
		);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePasswordChange(PasswordChangeEvent event) {
		String to = event.to();

		emailService.sendHtmlEmail(to, "Your Password Has Been Changed",
				"password-changed",
				Map.of(
						"appName", application.name(),
						"supportEmail", application.supportEmail(),
						"supportPhone", application.supportPhone(),
						"year", Year.now().getValue()
				)
		);
	}
}
