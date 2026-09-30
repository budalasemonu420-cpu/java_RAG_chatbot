package com.ragworkshop;

import com.ragworkshop.model.OllamaModel;
import com.ragworkshop.service.ModelSelectionService;
import com.ragworkshop.service.OllamaService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ModelSelectionServiceTest {
    @Test void prefersQwenOverLlama() { OllamaService ollama = mock(OllamaService.class); when(ollama.models()).thenReturn(List.of(new OllamaModel("llama3.2:3b", "llama"), new OllamaModel("qwen2.5:3b", "qwen"))); assertEquals("qwen2.5:3b", new ModelSelectionService(ollama).select().orElseThrow().name()); }
    @Test void fallsBackToLlama() { OllamaService ollama = mock(OllamaService.class); when(ollama.models()).thenReturn(List.of(new OllamaModel("llama3.2:3b", "llama"))); assertEquals("llama3.2:3b", new ModelSelectionService(ollama).select().orElseThrow().name()); }
    @Test void returnsEmptyWithoutSupportedModel() { OllamaService ollama = mock(OllamaService.class); when(ollama.models()).thenReturn(List.of(new OllamaModel("phi3:mini", "phi"))); assertEquals(true, new ModelSelectionService(ollama).select().isEmpty()); }
}
