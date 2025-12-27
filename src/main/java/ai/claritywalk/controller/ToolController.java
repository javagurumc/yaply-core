package ai.claritywalk.controller;

import ai.claritywalk.dto.ToolExecuteRequest;
import ai.claritywalk.dto.ToolExecuteResponse;
import ai.claritywalk.service.ToolExecutionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolExecutionService toolExecutionService;

    @PostMapping("/execute")
    public ToolExecuteResponse execute(@RequestBody @Valid ToolExecuteRequest req) {
        return toolExecutionService.execute(req);
    }

}
