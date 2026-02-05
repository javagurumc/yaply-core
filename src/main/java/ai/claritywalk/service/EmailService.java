package ai.claritywalk.service;

import ai.claritywalk.entity.Profile;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending emails to users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${claritywalk.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${claritywalk.email.from:noreply@claritywalk.ai}")
    private String fromEmail;

    @Value("${claritywalk.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Sends a welcome email to a newly registered user.
     *
     * @param profile The newly created user profile
     */
    public void sendWelcomeEmail(Profile profile) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping welcome email for: {}", profile.getEmail());
            return;
        }

        try {
            log.info("Sending welcome email to: {}", profile.getEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(profile.getEmail());
            helper.setSubject("Welcome to Clarity Walk! 🌟");

            String htmlContent = generateWelcomeEmailHtml(profile);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", profile.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", profile.getEmail(), e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    /**
     * Generates the HTML content for the welcome email.
     *
     * @param profile The user profile data
     * @return HTML string for email body
     */
    String generateWelcomeEmailHtml(Profile profile) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/welcome-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Format creation date
            String formattedDate = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());

            // Replace placeholders
            return template
                    .replace("{{email}}", profile.getEmail())
                    .replace("{{loginUrl}}", frontendUrl + "/login")
                    .replace("{{createdDate}}", formattedDate);

        } catch (IOException e) {
            log.error("Failed to load email template", e);
            throw new RuntimeException("Failed to generate email content", e);
        }
    }
}
