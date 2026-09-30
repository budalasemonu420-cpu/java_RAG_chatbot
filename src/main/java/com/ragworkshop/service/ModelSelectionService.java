package com.ragworkshop.service;

import com.ragworkshop.model.OllamaModel;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ModelSelectionService {
    private final OllamaService ollama;
    public ModelSelectionService(OllamaService ollama) { this.ollama = ollama; }
    public Optional<OllamaModel> select() { return ollama.models().stream().filter(model -> isFamily(model.name(), "qwen")).findFirst().or(() -> ollama.models().stream().filter(model -> isFamily(model.name(), "llama")).findFirst()); }
    public boolean isSupported(OllamaModel model) { return model != null && isFamily(model.name(), "qwen", "llama"); }
    private boolean isFamily(String name, String... families) { String lower = name.toLowerCase(Locale.ROOT); return Arrays.stream(families).anyMatch(lower::contains); }
}
