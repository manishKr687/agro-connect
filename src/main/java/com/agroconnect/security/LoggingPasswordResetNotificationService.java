package com.agroconnect.security;

import com.agroconnect.model.enums.PasswordResetChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class LoggingPasswordResetNotificationService implements PasswordResetNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectProvider<RestTemplate> restTemplateProvider;

    @Value("${app.password-reset.mail.enabled:false}")
    private boolean emailDeliveryEnabled;

    @Value("${app.password-reset.mail.from:}")
    private String fromAddress;

    @Value("${app.password-reset.mail.app-name:AgroConnect}")
    private String appName;

    @Value("${app.password-reset.sms.enabled:false}")
    private boolean smsDeliveryEnabled;

    @Value("${app.password-reset.sms.provider:TWILIO}")
    private String smsProvider;

    @Value("${app.password-reset.sms.from:}")
    private String smsFromNumber;

    @Value("${app.password-reset.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.password-reset.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.password-reset.sms.twilio.api-base-url:https://api.twilio.com}")
    private String twilioApiBaseUrl;

    public LoggingPasswordResetNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                                   ObjectProvider<RestTemplate> restTemplateProvider) {
        this.mailSenderProvider = mailSenderProvider;
        this.restTemplateProvider = restTemplateProvider;
    }

    @Override
    public void sendPasswordReset(PasswordResetChannel channel, String destination, String secret, String resetLink, long expiresInSeconds) {
        if (channel == PasswordResetChannel.EMAIL) {
            if (emailDeliveryEnabled && sendEmail(destination, resetLink, expiresInSeconds)) {
                return;
            }

            log.info("Password reset email link for {} expires in {} seconds: {}", destination, expiresInSeconds, resetLink);
            return;
        }

        if (smsDeliveryEnabled && sendSms(destination, secret, expiresInSeconds)) {
            return;
        }

        log.info("Password reset SMS OTP for {} expires in {} seconds: {}", destination, expiresInSeconds, secret);
    }

    private boolean sendEmail(String destination, String resetLink, long expiresInSeconds) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Password reset email delivery is enabled, but no JavaMailSender is configured. Falling back to log output.");
            return false;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Password reset email delivery is enabled, but app.password-reset.mail.from is blank. Falling back to log output.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(destination);
            helper.setFrom(fromAddress);
            helper.setSubject(appName + " password reset");
            helper.setText(buildEmailBody(resetLink, expiresInSeconds), false);
            mailSender.send(message);
            log.info("Password reset email sent to {}", destination);
            return true;
        } catch (MailException | jakarta.mail.MessagingException ex) {
            log.error("Failed to send password reset email to {}. Falling back to log output.", destination, ex);
            return false;
        }
    }

    private String buildEmailBody(String resetLink, long expiresInSeconds) {
        long expiresInMinutes = Math.max(1, expiresInSeconds / 60);
        return """
                You requested a password reset for %s.

                Use the link below to choose a new password:
                %s

                This link will expire in %d minute(s).

                If you did not request this change, you can ignore this email.
                """.formatted(appName, resetLink, expiresInMinutes);
    }

    private boolean sendSms(String destination, String otp, long expiresInSeconds) {
        if (!"TWILIO".equalsIgnoreCase(smsProvider)) {
            log.warn("Unsupported SMS provider '{}' configured for password reset. Falling back to log output.", smsProvider);
            return false;
        }

        RestTemplate restTemplate = restTemplateProvider.getIfAvailable();
        if (restTemplate == null) {
            log.warn("Password reset SMS delivery is enabled, but no RestTemplate is configured. Falling back to log output.");
            return false;
        }

        if (smsFromNumber == null || smsFromNumber.isBlank()
                || twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()) {
            log.warn("Password reset SMS delivery is enabled, but Twilio configuration is incomplete. Falling back to log output.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + buildBasicAuthToken(twilioAccountSid, twilioAuthToken));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", destination);
            form.add("From", smsFromNumber);
            form.add("Body", buildSmsBody(otp, expiresInSeconds));

            String url = twilioApiBaseUrl + "/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
            restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            log.info("Password reset SMS sent to {}", destination);
            return true;
        } catch (RestClientException ex) {
            log.error("Failed to send password reset SMS to {}. Falling back to log output.", destination, ex);
            return false;
        }
    }

    private String buildBasicAuthToken(String username, String password) {
        String raw = username + ":" + password;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String buildSmsBody(String otp, long expiresInSeconds) {
        long expiresInMinutes = Math.max(1, expiresInSeconds / 60);
        return "%s password reset OTP: %s. It expires in %d minute(s).".formatted(appName, otp, expiresInMinutes);
    }
}
