package ai.claritywalk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous task execution.
 * Enables @Async annotation support for non-blocking operations like email
 * sending.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
