package com.ragworkshop;

import com.ragworkshop.config.RagConfig;
import com.ragworkshop.model.ChatRequest;
import com.ragworkshop.model.OllamaModel;
import com.ragworkshop.model.RetrievedChunk;
import com.ragworkshop.service.ModelSelectionService;
import com.ragworkshop.service.OllamaService;
import com.ragworkshop.service.RagPipelineService;
import com.ragworkshop.service.RagPromptBuilder;
import com.ragworkshop.service.RetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagPipelineServiceTest {
    @Test
    void returnsGuidanceWithoutCallingModelWhenRetrievalFindsNothing() {
        RetrievalService retrieval = mock(RetrievalService.class);
        RagPromptBuilder prompts = mock(RagPromptBuilder.class);
        OllamaService ollama = mock(OllamaService.class);
        ModelSelectionService selector = mock(ModelSelectionService.class);
        RagConfig config = new RagConfig();
        config.setTopK(5);
        config.setSimilarityThreshold(0.6);
        when(retrieval.retrieve("question", 5, 0.6)).thenReturn(List.of());

        var response = new RagPipelineService(retrieval, prompts, ollama, selector, config)
                .chat(new ChatRequest("  question  ", null, null, null));

        assertEquals("I could not find relevant information in the uploaded documents. Please ask a question related to the uploaded document.", response.answer());
        assertNull(response.model());
        assertEquals(List.of(), response.sources());
        verify(selector, never()).select();
        verifyNoInteractions(prompts, ollama);
    }

    @Test
    void usesConfiguredDefaultsAndDelegatesRetrievedContext() {
        RetrievalService retrieval = mock(RetrievalService.class);
        RagPromptBuilder prompts = mock(RagPromptBuilder.class);
        OllamaService ollama = mock(OllamaService.class);
        ModelSelectionService selector = mock(ModelSelectionService.class);
        RagConfig config = new RagConfig();
        RetrievedChunk source = new RetrievedChunk("chunk-1", "guide.pdf", 2, 0.9, "Relevant text");
        List<RetrievedChunk> sources = List.of(source);
        OllamaModel model = new OllamaModel("qwen2.5:3b", "qwen");
        when(retrieval.retrieve("question", config.getTopK(), config.getSimilarityThreshold())).thenReturn(sources);
        when(selector.select()).thenReturn(Optional.of(model));
        when(prompts.build("question", sources)).thenReturn("built prompt");
        when(ollama.generate(model.name(), "built prompt", config.getTemperature())).thenReturn("grounded answer");

        var response = new RagPipelineService(retrieval, prompts, ollama, selector, config)
                .chat(new ChatRequest(" question ", null, null, null));

        assertEquals("grounded answer", response.answer());
        assertEquals(model.name(), response.model());
        assertEquals(sources, response.sources());
        verify(retrieval).retrieve("question", config.getTopK(), config.getSimilarityThreshold());
        verify(prompts).build("question", sources);
        verify(ollama).generate(model.name(), "built prompt", config.getTemperature());
    }

    @Test
    void usesRequestOverridesForRetrievalAndGeneration() {
        RetrievalService retrieval = mock(RetrievalService.class);
        RagPromptBuilder prompts = mock(RagPromptBuilder.class);
        OllamaService ollama = mock(OllamaService.class);
        ModelSelectionService selector = mock(ModelSelectionService.class);
        RagConfig config = new RagConfig();
        RetrievedChunk source = new RetrievedChunk("chunk-1", "guide.pdf", 2, 0.9, "Relevant text");
        List<RetrievedChunk> sources = List.of(source);
        OllamaModel model = new OllamaModel("llama3.2:3b", "llama");
        when(retrieval.retrieve("question", 2, 0.8)).thenReturn(sources);
        when(selector.select()).thenReturn(Optional.of(model));
        when(prompts.build("question", sources)).thenReturn("built prompt");
        when(ollama.generate(model.name(), "built prompt", 0.7)).thenReturn("answer");

        var response = new RagPipelineService(retrieval, prompts, ollama, selector, config)
                .chat(new ChatRequest(" question ", 2, 0.8, 0.7));

        assertEquals("answer", response.answer());
        verify(retrieval).retrieve("question", 2, 0.8);
        verify(ollama).generate(model.name(), "built prompt", 0.7);
    }

    @Test
    void failsClearlyWhenNoSupportedModelIsAvailable() {
        RetrievalService retrieval = mock(RetrievalService.class);
        RagPromptBuilder prompts = mock(RagPromptBuilder.class);
        OllamaService ollama = mock(OllamaService.class);
        ModelSelectionService selector = mock(ModelSelectionService.class);
        RagConfig config = new RagConfig();
        List<RetrievedChunk> sources = List.of(new RetrievedChunk("chunk-1", "guide.pdf", 1, 0.9, "Relevant text"));
        when(retrieval.retrieve("question", config.getTopK(), config.getSimilarityThreshold())).thenReturn(sources);
        when(selector.select()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> new RagPipelineService(retrieval, prompts, ollama, selector, config)
                .chat(new ChatRequest("question", null, null, null)));

        verifyNoInteractions(prompts, ollama);
    }
}