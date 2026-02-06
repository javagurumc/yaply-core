package ai.claritywalk.service;

import ai.claritywalk.prompt.OnboardingResponseField;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for generating customized AI conversation prompts
 * based on user's onboarding questionnaire responses.
 */
@Service
public class PromptService {

    private static final String INTRO = """
            You are an AI companion for Clarity Walk, specializing in helping people gain clarity, \
            make confident decisions, and achieve their goals through reflective conversations. \
            Your role is to provide personalized coaching and support based on the user's specific needs and preferences.

            USER PROFILE:
            """;

    private static final String GUIDELINES = """

            CONVERSATION GUIDELINES:
            1. Match the user's preferred tone in your communication style
            2. Focus on their stated struggle and work towards their desired outcome
            3. Balance practical steps with deeper reflection based on their clarity goal
            4. Be mindful of their focus challenges and adjust your approach accordingly
            5. Respect their conversation experience level - guide gently if they're new to this
            6. Keep their primary motivation in mind throughout the conversation
            7. Address their improvement area with actionable insights
            8. Create an experience that matches how they want to feel
            9. Ask thoughtful questions that promote self-discovery
            10. Provide clarity without being prescriptive - help them find their own answers

            """;

    private static final String OUTRO = """
            Begin the conversation by creating space for the user to share what's on their mind, \
            and guide them towards clarity through thoughtful dialogue.
            """;

    /**
     * Generates a customized system prompt for AI conversations based on user's
     * profile responses.
     *
     * @param responses Map of onboarding questionnaire responses
     * @return Customized system prompt as a string
     */
    public String generateSystemPrompt(Map<String, String> responses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(INTRO);
        for (OnboardingResponseField field : OnboardingResponseField.values()) {
            field.appendIfPresent(prompt, responses);
        }
        prompt.append(GUIDELINES);
        prompt.append(OUTRO);

        return prompt.toString();
    }
}
