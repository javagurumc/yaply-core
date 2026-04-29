package ai.yaply.service;

import ai.yaply.dto.CreateKnowledgeBaseDocumentRequest;
import ai.yaply.dto.KnowledgeBaseDocumentResponse;
import ai.yaply.dto.UpdateKnowledgeBaseDocumentRequest;
import ai.yaply.entity.KbDocument;
import ai.yaply.entity.KbScope;
import ai.yaply.entity.Profile;
import ai.yaply.repo.KbDocumentRepository;
import ai.yaply.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseDocumentService {

    private static final String DEFAULT_SOURCE = "user_prompt_context";
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 40;

    private final KbDocumentRepository kbDocumentRepository;
    private final KbIngestService kbIngestService;
    private final ProfileRepository profileRepository;

    public List<KnowledgeBaseDocumentResponse> list(Authentication auth) {
        String userId = resolveUserId(auth);
        return kbDocumentRepository.findByScopeAndUserId(KbScope.USER, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public KnowledgeBaseDocumentResponse create(CreateKnowledgeBaseDocumentRequest request, Authentication auth) {
        String userId = resolveUserId(auth);
        String title = validateTitle(request.title());
        String content = validateContent(request.content());
        List<String> tags = validateTags(request.tags());
        String source = normalizeSource(request.source());
        int version = request.version() != null ? request.version() : 1;

        UUID documentId = kbIngestService.ingestDocument(
                KbScope.USER,
                userId,
                source,
                title,
                content,
                tags,
                version
        );

        KbDocument document = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Created KB document not found"));
        ensureOwnership(document, userId);
        return toResponse(document);
    }

    public KnowledgeBaseDocumentResponse update(UUID id, UpdateKnowledgeBaseDocumentRequest request, Authentication auth) {
        String userId = resolveUserId(auth);
        KbDocument existing = kbDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base document not found"));
        ensureOwnership(existing, userId);

        String title = validateTitle(request.title());
        String content = validateContent(request.content());
        List<String> tags = validateTags(request.tags());
        String source = normalizeSource(request.source());
        int version = request.version() != null ? request.version() : existing.getVersion();

        UUID documentId = kbIngestService.ingestDocument(
                KbScope.USER,
                userId,
                source,
                title,
                content,
                tags,
                version
        );

        KbDocument updated = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Updated KB document not found"));
        ensureOwnership(updated, userId);
        return toResponse(updated);
    }

    public void delete(UUID id, Authentication auth) {
        String userId = resolveUserId(auth);
        KbDocument existing = kbDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base document not found"));
        ensureOwnership(existing, userId);
        kbIngestService.deleteDocument(id);
    }

    private String resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        Profile profile = profileRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + auth.getName()));
        return profile.getUserId();
    }

    private void ensureOwnership(KbDocument document, String userId) {
        if (document.getScope() != KbScope.USER || document.getUserId() == null || !document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Knowledge base document does not belong to authenticated user");
        }
    }

    private KnowledgeBaseDocumentResponse toResponse(KbDocument doc) {
        List<String> tags = doc.getTags() == null ? List.of() : Arrays.asList(doc.getTags());
        long chunkCount = kbIngestService.getChunkCount(doc.getId());
        return new KnowledgeBaseDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getSource(),
                tags,
                doc.getVersion(),
                chunkCount,
                doc.getUpdatedAt()
        );
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        String normalized = title.trim();
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
        return normalized;
    }

    private String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content cannot exceed " + MAX_CONTENT_LENGTH + " characters");
        }
        return normalized;
    }

    private List<String> validateTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        if (tags.size() > MAX_TAGS) {
            throw new IllegalArgumentException("Cannot have more than " + MAX_TAGS + " tags");
        }
        List<String> normalized = tags.stream()
                .map(tag -> tag == null ? "" : tag.trim())
                .filter(tag -> !tag.isBlank())
                .toList();
        for (String tag : normalized) {
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("Tag cannot exceed " + MAX_TAG_LENGTH + " characters");
            }
        }
        return normalized;
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return DEFAULT_SOURCE;
        }
        return source.trim();
    }
}
