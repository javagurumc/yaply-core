package ai.claritywalk.service;

import ai.claritywalk.config.KbConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating text embeddings using OpenAI API.
 * Supports batch processing, retry logic, and vector format conversion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final KbConfig kbConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${claritywalk.openai.apiKey}")
    private String openaiApiKey;

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * Generate embedding for a single text.
     */
    public float[] embed(String text) {
        List<float[]> embeddings = embedBatch(List.of(text));
        return embeddings.isEmpty() ? new float[0] : embeddings.get(0);
    }

    /**
     * Generate embeddings for a batch of texts.
     * Automatically splits into multiple API calls if batch size exceeds limit.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> allEmbeddings = new ArrayList<>();
        int batchSize = kbConfig.getEmbedding().getBatchSize();

        // Process in batches
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            log.info("Generating embeddings for batch {}/{} ({} texts)",
                    (i / batchSize) + 1, (texts.size() + batchSize - 1) / batchSize, batch.size());

            List<float[]> batchEmbeddings = embedBatchInternal(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }

    /**
     * Internal method to embed a single batch with retry logic.
     */
    private List<float[]> embedBatchInternal(List<String> texts) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callOpenAIEmbeddingsAPI(texts);
            } catch (Exception e) {
                log.warn("Embedding attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Embedding interrupted", ie);
                    }
                } else {
                    throw new RuntimeException("Failed to generate embeddings after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        return List.of();
    }

    /**
     * Call OpenAI Embeddings API.
     */
    private List<float[]> callOpenAIEmbeddingsAPI(List<String> texts) {
        try {
            // Build request
            Map<String, Object> requestBody = Map.of(
                    "model", kbConfig.getEmbedding().getModel(),
                    "input", texts,
                    "dimensions", kbConfig.getEmbedding().getDimension());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody),
                    headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("OpenAI API error: " + response.getStatusCode());
            }

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingNode = item.get("embedding");
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(embedding);
            }

            log.info("Successfully generated {} embeddings", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("Error calling OpenAI Embeddings API", e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }

    /**
     * Convert float[] embedding to pgvector format: "[0.1, 0.2, ...]"
     */
    public String formatVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse pgvector format string back to float[].
     */
    public float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[0];
        }

        String cleaned = vectorStr.replaceAll("[\\[\\]]", "").trim();
        String[] parts = cleaned.split(",");

        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
