package ai.claritywalk.entity;

/**
 * Knowledge Base scope enumeration.
 * Defines the three types of knowledge storage in the system.
 */
public enum KbScope {
    /**
     * Shared knowledge accessible to all users.
     * Examples: method handbook, FAQ, safety guidelines
     */
    GLOBAL,

    /**
     * Versioned program content.
     * Examples: exercise templates, weekly plans, program structures
     */
    PROGRAM,

    /**
     * User-specific personal memory.
     * Examples: onboarding responses, walk summaries, personal insights
     */
    USER
}
