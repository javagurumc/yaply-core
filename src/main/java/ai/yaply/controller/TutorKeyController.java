package ai.yaply.controller;
import ai.yaply.dto.*;
import ai.yaply.service.TutorCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/tutor/api-key")
@RequiredArgsConstructor
public class TutorKeyController {
    private final TutorCredentialService service;
    @GetMapping public ResponseEntity<TutorKeyStatus> status(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(auth));
    }
    @PutMapping public ResponseEntity<TutorKeyStatus> save(@RequestBody SaveTutorKeyRequest request, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.save(auth, request.getApiKey()));
    }
    @DeleteMapping public ResponseEntity<Void> remove(Authentication auth) {
        service.remove(auth);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
