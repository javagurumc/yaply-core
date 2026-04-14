package ai.yaply.controller;

import ai.yaply.dto.ToolExecuteRequest;
import ai.yaply.dto.ToolExecuteResponse;
import ai.yaply.service.ToolExecutionService;
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
