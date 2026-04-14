package ai.yaply.prompt;

import java.util.Map;

public enum OnboardingResponseField {
    STRUGGLE("struggle", "Current Struggle"),
    CLARITY_GOAL("clarity_goal", "What They Seek"),
    TONE("tone", "Preferred Tone"),
    FOCUS_HARDEST("focus_hardest", "Focus Challenge"),
    MOTIVATION("motivation", "Primary Motivation"),
    CONVERSATION_MODE("conversation_mode", "Conversation Experience"),
    OUTCOME("outcome", "Desired Outcome"),
    FREQUENCY("frequency", "Engagement Frequency"),
    IMPROVEMENT_AREA("improvement_area", "Improvement Area"),
    EXPERIENCE_FEEL("experience_feel", "Desired Experience");

    private final String key;
    private final String label;

    OnboardingResponseField(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public void appendIfPresent(StringBuilder prompt, Map<String, String> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        String value = responses.get(key);
        if (value == null || value.isBlank()) {
            return;
        }
        prompt.append("- ").append(label).append(": ").append(value.strip()).append("\n");
    }
}
