package ai.yaply.event;

import ai.yaply.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for user-related events and triggers appropriate actions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    /**
     * Handles UserCreatedEvent by sending a welcome email.
     * Runs asynchronously to avoid blocking the registration process.
     */
    @Async
    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        try {
            log.info("Received UserCreatedEvent for email: {}", event.getProfile().getEmail());
            emailService.sendWelcomeEmail(event.getProfile());
        } catch (Exception e) {
            // Log error but don't propagate - email failure shouldn't affect registration
            log.error("Failed to send welcome email for user: {}", event.getProfile().getEmail(), e);
        }
    }
}
