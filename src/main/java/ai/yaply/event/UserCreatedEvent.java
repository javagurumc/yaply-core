package ai.yaply.event;

import ai.yaply.entity.Profile;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new user profile is created.
 * This event is used to trigger post-registration actions like sending welcome
 * emails.
 */
@Getter
public class UserCreatedEvent extends ApplicationEvent {

    private final Profile profile;

    public UserCreatedEvent(Object source, Profile profile) {
        super(source);
        this.profile = profile;
    }
}
