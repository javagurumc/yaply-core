package ai.yaply.controller;

import ai.yaply.dto.CreateKnowledgeBaseDocumentRequest;
import ai.yaply.dto.KnowledgeBaseDocumentResponse;
import ai.yaply.dto.UpdateKnowledgeBaseDocumentRequest;
import ai.yaply.service.KnowledgeBaseDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/kb/documents")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @GetMapping
    public List<KnowledgeBaseDocumentResponse> list(Authentication auth) {
        return knowledgeBaseDocumentService.list(auth);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateKnowledgeBaseDocumentRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(knowledgeBaseDocumentService.create(request, auth));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                    @RequestBody UpdateKnowledgeBaseDocumentRequest request,
                                    Authentication auth) {
        try {
            return ResponseEntity.ok(knowledgeBaseDocumentService.update(id, request, auth));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, Authentication auth) {
        try {
            knowledgeBaseDocumentService.delete(id, auth);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}
