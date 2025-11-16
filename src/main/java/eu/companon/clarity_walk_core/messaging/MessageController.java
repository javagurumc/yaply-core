package eu.companon.clarity_walk_core.messaging;

import eu.companon.clarity_walk_core.messaging.dto.MessageCreateRequestDto;
import eu.companon.clarity_walk_core.messaging.dto.MessageDetailsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final ConcurrentMap<UUID, MessageDetailsDto> store = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<MessageDetailsDto> createMessage(@RequestBody MessageCreateRequestDto request,
                                                           UriComponentsBuilder uriBuilder) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var message = new MessageDetailsDto(id, now, request.content());
        store.put(id, message);

        URI location = uriBuilder.path("/messages/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(message);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageDetailsDto> getMessage(@PathVariable Long id) {
        var msg = store.get(id);
        if (msg == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(msg);
    }

    @GetMapping
    public List<MessageDetailsDto> listMessages() {
        return new ArrayList<>(store.values());
    }


}
