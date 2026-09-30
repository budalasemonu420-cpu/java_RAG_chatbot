package com.ragworkshop.controller;

import com.ragworkshop.model.OllamaModel;
import com.ragworkshop.model.SystemStatus;
import com.ragworkshop.repository.InMemoryVectorStore;
import com.ragworkshop.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class SystemController {
    private final OllamaService ollama; private final ModelSelectionService selector; private final DocumentService documents; private final InMemoryVectorStore store;
    public SystemController(OllamaService ollama, ModelSelectionService selector, DocumentService documents, InMemoryVectorStore store) { this.ollama = ollama; this.selector = selector; this.documents = documents; this.store = store; }
    @GetMapping("/system/status") public SystemStatus status() { Optional<OllamaModel> model = selector.select(); return new SystemStatus(ollama.connected(), model.map(OllamaModel::name).orElse(null), model.isPresent()); }
    @GetMapping("/system/models") public List<OllamaModel> models() { return ollama.models(); }
    @GetMapping("/index/status") public Map<String, Object> index() { return Map.of("documentCount", documents.documentCount(), "chunkCount", store.size(), "embeddingStatus", store.size() > 0 ? "READY" : "EMPTY"); }
}
