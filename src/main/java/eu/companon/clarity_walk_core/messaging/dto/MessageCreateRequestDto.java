package eu.companon.clarity_walk_core.messaging.dto;

import java.util.UUID;

public record MessageCreateRequestDto(UUID chatId, String content) {
}
