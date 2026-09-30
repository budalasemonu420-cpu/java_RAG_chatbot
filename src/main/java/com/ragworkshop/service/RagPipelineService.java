package com.ragworkshop.service;

import com.ragworkshop.config.RagConfig;
import com.ragworkshop.model.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RagPipelineService {
    private static final String NO_RESULT = "I could not find relevant information in the uploaded documents. Please ask a question related to the uploaded document.";
    private final RetrievalService retrieval; private final RagPromptBuilder prompts; private final OllamaService ollama; private final ModelSelectionService selector; private final RagConfig config;
    public RagPipelineService(RetrievalService retrieval, RagPromptBuilder prompts, OllamaService ollama, ModelSelectionService selector, RagConfig config) { this.retrieval = retrieval; this.prompts = prompts; this.ollama = ollama; this.selector = selector; this.config = config; }
    public ChatResponse chat(ChatRequest request) {
        List<RetrievedChunk> sources = retrieval.retrieve(request.question().trim(), request.topK() == null ? config.getTopK() : request.topK(), request.similarityThreshold() == null ? config.getSimilarityThreshold() : request.similarityThreshold());
        if (sources.isEmpty()) return new ChatResponse(NO_RESULT, null, sources);
        OllamaModel model = selector.select().orElseThrow(() -> new IllegalStateException("No supported Qwen or Llama model is installed. Install one with Ollama and try again."));
        return new ChatResponse(ollama.generate(model.name(), prompts.build(request.question().trim(), sources), request.temperature() == null ? config.getTemperature() : request.temperature()), model.name(), sources);
    }
}
