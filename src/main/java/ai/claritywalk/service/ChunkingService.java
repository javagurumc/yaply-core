package ai.claritywalk.service;

import ai.claritywalk.config.KbConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for chunking text into smaller pieces for embedding.
 * Uses token-based estimation and overlap for context continuity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final KbConfig kbConfig;

    // Simple heuristic: ~4 characters = 1 token (approximate)
    private static final double CHARS_PER_TOKEN = 4.0;

    // Paragraph separator pattern
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n\n+");

    /**
     * Chunk text into smaller pieces with overlap.
     *
     * @param content Text to chunk
     * @return List of text chunks
     */
    public List<String> chunkText(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        int maxChunkTokens = kbConfig.getChunking().getMaxChunkSize();
        int overlapTokens = kbConfig.getChunking().getOverlapSize();

        // Convert tokens to approximate character counts
        int maxChunkChars = (int) (maxChunkTokens * CHARS_PER_TOKEN);
        int overlapChars = (int) (overlapTokens * CHARS_PER_TOKEN);

        // Split by paragraphs first
        String[] paragraphs = PARAGRAPH_PATTERN.split(content.trim());

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String previousOverlap = "";

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // If adding this paragraph would exceed max size, finalize current chunk
            if (currentChunk.length() + paragraph.length() > maxChunkChars && currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                chunks.add(chunkText);

                // Extract overlap from end of chunk for next chunk
                previousOverlap = extractOverlap(chunkText, overlapChars);
                currentChunk = new StringBuilder(previousOverlap);

                // Add separator after overlap
                if (!previousOverlap.isEmpty()) {
                    currentChunk.append("\n\n");
                }
            }

            // Add paragraph to current chunk
            if (currentChunk.length() > 0 && !currentChunk.toString().endsWith("\n\n")) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        // Add final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Chunked {} characters into {} chunks (max {} tokens/chunk, {} tokens overlap)",
                content.length(), chunks.size(), maxChunkTokens, overlapTokens);

        return chunks;
    }

    /**
     * Extract the last N characters for overlap.
     * Tries to break at word boundaries.
     */
    private String extractOverlap(String text, int overlapChars) {
        if (text.length() <= overlapChars) {
            return text;
        }

        int startPos = text.length() - overlapChars;

        // Try to find a word boundary (space) to avoid cutting mid-word
        int spacePos = text.indexOf(' ', startPos);
        if (spacePos > 0 && spacePos < text.length() - 1) {
            startPos = spacePos + 1;
        }

        return text.substring(startPos).trim();
    }

    /**
     * Estimate tokens in text using simple heuristic.
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
