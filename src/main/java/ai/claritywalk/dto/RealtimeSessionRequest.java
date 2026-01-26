package ai.claritywalk.dto;

import java.util.List;
import java.util.Map;

public record RealtimeSessionRequest(
        String model,
        List<String> modalities,
        Object voice,
        String input_audio_format,
        String output_audio_format,
        Map<String, Object> input_audio_transcription,
        String instructions
) {}
