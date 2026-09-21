package ai.yaply.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateAgentRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description) {
    public CreateAgentRequest {
        name = name == null ? null : name.strip();
        description = description == null ? "" : description.strip();
    }
}
