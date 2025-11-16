package eu.companon.clarity_walk_core.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDetailsDto(
        UUID id,
        Instant now,
        String content){
}
