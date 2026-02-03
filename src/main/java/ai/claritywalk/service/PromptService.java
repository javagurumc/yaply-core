package ai.claritywalk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for generating customized AI conversation prompts
 * based on user's onboarding questionnaire responses.
 */
@Slf4j
@Service
public class PromptService {

    /**
     * Generates a customized system prompt for AI conversations based on user's
     * profile responses.
     *
     * @param responses Map of onboarding questionnaire responses
     * @return Customized system prompt as a string
     */
    public String generateSystemPrompt(Map<String, String> responses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI companion for Clarity Walk, specializing in helping people gain clarity, ");
        prompt.append("make confident decisions, and achieve their goals through reflective conversations. ");
        prompt.append(
                "Your role is to provide personalized coaching and support based on the user's specific needs and preferences.\n\n");

        prompt.append("USER PROFILE:\n");

        // Main struggle
        if (responses.containsKey("struggle")) {
            prompt.append("- Current Struggle: ").append(responses.get("struggle")).append("\n");
        }

        // Clarity goal - what they want from conversations
        if (responses.containsKey("clarity_goal")) {
            prompt.append("- What They Seek: ").append(responses.get("clarity_goal")).append("\n");
        }

        // Preferred communication tone
        if (responses.containsKey("tone")) {
            prompt.append("- Preferred Tone: ").append(responses.get("tone")).append("\n");
        }

        // What's hardest to focus on
        if (responses.containsKey("focus_hardest")) {
            prompt.append("- Focus Challenge: ").append(responses.get("focus_hardest")).append("\n");
        }

        // Main motivation
        if (responses.containsKey("motivation")) {
            prompt.append("- Primary Motivation: ").append(responses.get("motivation")).append("\n");
        }

        // Experience with reflective conversations
        if (responses.containsKey("conversation_mode")) {
            prompt.append("- Conversation Experience: ").append(responses.get("conversation_mode")).append("\n");
        }

        // Desired outcome
        if (responses.containsKey("outcome")) {
            prompt.append("- Desired Outcome: ").append(responses.get("outcome")).append("\n");
        }

        // How often they want to engage
        if (responses.containsKey("frequency")) {
            prompt.append("- Engagement Frequency: ").append(responses.get("frequency")).append("\n");
        }

        // Area they want to improve
        if (responses.containsKey("improvement_area")) {
            prompt.append("- Improvement Area: ").append(responses.get("improvement_area")).append("\n");
        }

        // How they want the experience to feel
        if (responses.containsKey("experience_feel")) {
            prompt.append("- Desired Experience: ").append(responses.get("experience_feel")).append("\n");
        }

        prompt.append("\nCONVERSATION GUIDELINES:\n");
        prompt.append("1. Match the user's preferred tone in your communication style\n");
        prompt.append("2. Focus on their stated struggle and work towards their desired outcome\n");
        prompt.append("3. Balance practical steps with deeper reflection based on their clarity goal\n");
        prompt.append("4. Be mindful of their focus challenges and adjust your approach accordingly\n");
        prompt.append("5. Respect their conversation experience level - guide gently if they're new to this\n");
        prompt.append("6. Keep their primary motivation in mind throughout the conversation\n");
        prompt.append("7. Address their improvement area with actionable insights\n");
        prompt.append("8. Create an experience that matches how they want to feel\n");
        prompt.append("9. Ask thoughtful questions that promote self-discovery\n");
        prompt.append("10. Provide clarity without being prescriptive - help them find their own answers\n\n");

        prompt.append("Begin the conversation by creating space for the user to share what's on their mind, ");
        prompt.append("and guide them towards clarity through thoughtful dialogue.");

        return prompt.toString();
    }
}
