package com.alok.earthquake_soap_service.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Value("${azure.communication.connection-string:}")
    private String connectionString;

    @Value("${azure.communication.sender-address:DoNotReply@azurecomm.net}")
    private String senderAddress;

    private EmailClient emailClient;

    @PostConstruct
    public void init() {
        if (connectionString != null && !connectionString.trim().isEmpty() && !connectionString.contains("your-endpoint")) {
            try {
                this.emailClient = new EmailClientBuilder()
                        .connectionString(connectionString.trim())
                        .buildClient();
                logger.info("Azure Communication Services EmailClient successfully initialized with sender '{}'", senderAddress);
            } catch (Exception e) {
                logger.warn("Failed to initialize Azure Communication Services EmailClient: {}. Falling back to simulated email mode.", e.getMessage());
                this.emailClient = null;
            }
        } else {
            logger.info("No valid ACS_CONNECTION_STRING provided. EmailNotificationService will run in simulated (mock) mode.");
            this.emailClient = null;
        }
    }

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Sends an email notification to the recipient.
     * If recipient is not a valid email, logs a warning and gracefully skips.
     * If Azure Communication Services client is not initialized, logs a simulated email message.
     */
    public boolean sendNotification(String recipient, String subscriberName, String subject, String plainTextBody, String htmlBody) {
        if (!isValidEmail(recipient)) {
            logger.warn("Subscriber contact '{}' for '{}' is not a valid email address; skipping email dispatch.",
                    recipient, subscriberName);
            return false;
        }

        String targetEmail = recipient.trim();

        if (emailClient == null) {
            logger.info("[MOCK ALERT EMAIL] Real email skipped (ACS not configured). To: <{}>, Name: '{}', Subject: '{}'\nMessage:\n{}",
                    targetEmail, subscriberName, subject, plainTextBody);
            return true;
        }

        try {
            EmailMessage message = new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(targetEmail)
                    .setSubject(subject)
                    .setBodyPlainText(plainTextBody)
                    .setBodyHtml(htmlBody != null ? htmlBody : "<pre>" + plainTextBody + "</pre>");

            logger.info("Dispatching email alert via Azure Communication Services to <{}> for subject '{}'...",
                    targetEmail, subject);
            emailClient.beginSend(message);
            logger.info("Azure Communication Services email alert successfully queued for <{}>.", targetEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email alert via Azure Communication Services to <{}>: {}", targetEmail, e.getMessage(), e);
            return false;
        }
    }
}
