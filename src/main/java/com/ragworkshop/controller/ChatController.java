package com.ragworkshop.controller;

import com.ragworkshop.model.*;
import com.ragworkshop.service.RagPipelineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final RagPipelineService pipeline;
    public ChatController(RagPipelineService pipeline) { this.pipeline = pipeline; }
    @PostMapping public ChatResponse chat(@Valid @RequestBody ChatRequest request) { return pipeline.chat(request); }
    @DeleteMapping("/history") public Map<String, Object> clearHistory() { return Map.of("success", true); }
}
