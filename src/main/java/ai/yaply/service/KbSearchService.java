package ai.yaply.service;

import ai.yaply.config.KbConfig;
import ai.yaply.dto.KbSearchResult;
import ai.yaply.dto.KbSearchResult.ChunkResult;
import ai.yaply.entity.KbScope;
import ai.yaply.repo.KbChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for retrieving knowledge from the knowledge base using vector
 * similarity search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbSearchService {

    private final KbChunkRepository chunkRepo;
    private final EmbeddingService embeddingService;
    private final ChunkingService chunkingService;
    private final KbConfig kbConfig;

    /**
     * Search the knowledge base for relevant chunks.
     *
     * @param query  Search query in natural language
     * @param scope  Knowledge scope to search
     * @param userId User ID for USER scope (null for others)
     * @param topK   Number of results to return
     * @param source Optional source filter (null to skip)
     * @return Search results with top-K most relevant chunks
     */
    public KbSearchResult search(
            String query,
            KbScope scope,
            String userId,
            Integer topK,
            String source) {
        log.info("Searching KB: query='{}', scope={}, userId={}, topK={}, source={}",
                query, scope, userId, topK, source);

        // Validate inputs
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        if (scope == KbScope.USER && (userId == null || userId.isBlank())) {
            throw new IllegalArgumentException("userId is required for USER scope");
        }

        // Apply topK limits
        int effectiveTopK = topK != null ? topK : kbConfig.getRetrieval().getDefaultTopK();
        effectiveTopK = Math.min(effectiveTopK, kbConfig.getRetrieval().getMaxTopK());

        // 1. Generate query embedding
        float[] queryEmbedding = embeddingService.embed(query);
        String queryEmbeddingStr = embeddingService.formatVector(queryEmbedding);

        // 2. Execute vector similarity search
        List<Object[]> rawResults = chunkRepo.searchSimilar(
                queryEmbeddingStr,
                scope.name(),
                userId,
                source,
                effectiveTopK * 2 // Fetch extra for post-processing
        );

        log.info("Found {} raw results from vector search", rawResults.size());

        // 3. Map to DTOs
        List<ChunkResult> chunks = rawResults.stream()
                .map(this::mapToChunkResult)
                .collect(Collectors.toList());

        // 4. Post-process: deduplicate and cap tokens
        chunks = postProcessResults(chunks);

        log.info("Returning {} results after post-processing", chunks.size());

        return KbSearchResult.create(chunks);
    }

    /**
     * Map database Object[] to ChunkResult DTO.
     * Object[] structure: [chunkId, documentId, chunkIndex, chunkText, embedding,
     * createdAt,
     * scope, userId, source, title, tags, score]
     */
    private ChunkResult mapToChunkResult(Object[] row) {
        return new ChunkResult(
                UUID.fromString(row[0].toString()), // chunkId
                UUID.fromString(row[1].toString()), // documentId
                row[9].toString(), // title
                row[8].toString(), // source
                row[3].toString(), // chunkText
                ((Number) row[11]).doubleValue() // score
        );
    }

    /**
     * Post-process results:
     * 1. Deduplicate chunks from same document (keep highest score per doc, max 3
     * chunks)
     * 2. Cap total tokens to configured maximum
     * 3. Return top-K results
     */
    private List<ChunkResult> postProcessResults(List<ChunkResult> results) {
        // Group by document ID
        Map<UUID, List<ChunkResult>> byDocument = results.stream()
                .collect(Collectors.groupingBy(ChunkResult::documentId));

        // Keep top 3 chunks per document (highest scores)
        List<ChunkResult> deduplicated = byDocument.values().stream()
                .flatMap(chunks -> chunks.stream()
                        .sorted(Comparator.comparingDouble(ChunkResult::score).reversed())
                        .limit(3)) // Max 3 chunks per document
                .sorted(Comparator.comparingDouble(ChunkResult::score).reversed())
                .collect(Collectors.toList());

        // Cap total tokens
        int maxTokens = kbConfig.getRetrieval().getMaxReturnedTokens();
        List<ChunkResult> capped = new ArrayList<>();
        int totalTokens = 0;

        for (ChunkResult chunk : deduplicated) {
            int chunkTokens = chunkingService.estimateTokens(chunk.text());

            if (totalTokens + chunkTokens > maxTokens) {
                log.info("Capping results at {} chunks due to token limit ({}/{})",
                        capped.size(), totalTokens, maxTokens);
                break;
            }

            capped.add(chunk);
            totalTokens += chunkTokens;
        }

        // Limit to effective topK
        int topK = kbConfig.getRetrieval().getDefaultTopK();
        return capped.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }
}
