package com.zamazor.market.mail.service;

import com.zamazor.market.mail.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final MailProperties mailProperties;

	@Async
	public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name()
			);

			Context context = new Context();
			context.setVariables(variables);
			String htmlContent = templateEngine.process("emails/%s".formatted(templateName), context);

			helper.setTo(to);
			helper.setFrom(mailProperties.fromAddress(), mailProperties.personalName());
			helper.setSubject(subject);
			helper.setText(htmlContent, true);

			mailSender.send(message);
			log.info("Email successfully sent to {}", to);

		} catch (MessagingException | UnsupportedEncodingException e) {
			log.error("Failed to send email to {}", to, e);
		}
	}
}
