package ai.yaply.controller;
import ai.yaply.service.CredentialFailure;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
@RestControllerAdvice(assignableTypes = TutorKeyController.class)
public class TutorKeyExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> error(Exception error) {
        int status = 500;
        String message = "Unable to update API key settings. Please try again.";
        if (error instanceof CredentialFailure failure) { status = 503; message = failure.getMessage(); }
        else if (error instanceof HttpMessageNotReadableException) { status = 400; message = "Invalid API key request."; }
        else if (error instanceof ResponseStatusException response) {
            status = response.getStatusCode().value();
            message = status == 401 ? "Authentication required." : "Enter a valid API key without whitespace (maximum 4096 characters).";
        }
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message", message));
    }
}
