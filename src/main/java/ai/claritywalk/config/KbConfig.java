package ai.claritywalk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Knowledge Base configuration properties.
 * Maps to claritywalk.kb in application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = "claritywalk.kb")
@Getter
@Setter
public class KbConfig {

    private Embedding embedding = new Embedding();
    private Chunking chunking = new Chunking();
    private Retrieval retrieval = new Retrieval();

    @Getter
    @Setter
    public static class Embedding {
        private String model = "text-embedding-3-large";
        private int dimension = 3072;
        private int batchSize = 100;
    }

    @Getter
    @Setter
    public static class Chunking {
        private int maxChunkSize = 800;
        private int overlapSize = 100;
    }

    @Getter
    @Setter
    public static class Retrieval {
        private int defaultTopK = 5;
        private int maxTopK = 20;
        private int maxReturnedTokens = 2000;
    }
}
