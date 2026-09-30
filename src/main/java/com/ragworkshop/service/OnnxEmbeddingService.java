package com.ragworkshop.service;

import ai.onnxruntime.*;
import com.ragworkshop.config.RagConfig;
import com.ragworkshop.exception.DocumentProcessingException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class OnnxEmbeddingService implements EmbeddingService {
    private static final int MAX_TOKENS = 128;
    private final RagConfig config;
    private OrtEnvironment environment;
    private OrtSession session;
    private Map<String, Long> vocabulary = Map.of();
    public OnnxEmbeddingService(RagConfig config) { this.config = config; }

    @PostConstruct
    void initialize() {
        try {
            Path model = Path.of(config.getEmbeddingModelPath());
            Path vocab = Path.of(config.getTokenizerPath());
            if (!Files.exists(model) || !Files.exists(vocab)) return;
            environment = OrtEnvironment.getEnvironment();
            session = environment.createSession(model.toString(), new OrtSession.SessionOptions());
            Map<String, Long> loaded = new HashMap<>();
            List<String> lines = Files.readAllLines(vocab);
            for (int i = 0; i < lines.size(); i++) loaded.put(lines.get(i).trim(), (long) i);
            vocabulary = loaded;
        } catch (Exception exception) {
            throw new DocumentProcessingException("Unable to load the local ONNX embedding model.", exception);
        }
    }

    @Override public float[] generateEmbedding(String text) {
        if (session == null) throw new DocumentProcessingException("Embedding model is not installed. See README.md for the local MiniLM model setup.");
        try (OnnxTensor ids = OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenIds(text)), new long[]{1, MAX_TOKENS});
             OnnxTensor mask = OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask(text)), new long[]{1, MAX_TOKENS});
             OnnxTensor types = OnnxTensor.createTensor(environment, LongBuffer.wrap(new long[MAX_TOKENS]), new long[]{1, MAX_TOKENS});
             OrtSession.Result output = session.run(Map.of("input_ids", ids, "attention_mask", mask, "token_type_ids", types))) {
            Object value = output.get(0).getValue();
            float[][][] hidden = (float[][][]) value;
            float[] vector = new float[hidden[0][0].length];
            int count = 0;
            for (int token = 0; token < MAX_TOKENS; token++) if (attentionMask(text)[token] == 1) { for (int i = 0; i < vector.length; i++) vector[i] += hidden[0][token][i]; count++; }
            for (int i = 0; i < vector.length; i++) vector[i] /= Math.max(1, count);
            normalize(vector);
            return vector;
        } catch (Exception exception) { throw new DocumentProcessingException("Local embedding generation failed.", exception); }
    }

    @Override public List<float[]> generateEmbeddings(List<String> texts) { return texts.stream().map(this::generateEmbedding).toList(); }
    private long[] tokenIds(String text) { long[] ids = new long[MAX_TOKENS]; List<String> tokens = tokens(text); for (int i = 0; i < Math.min(MAX_TOKENS, tokens.size()); i++) ids[i] = vocabulary.getOrDefault(tokens.get(i), vocabulary.getOrDefault("[UNK]", 100L)); return ids; }
    private long[] attentionMask(String text) { long[] mask = new long[MAX_TOKENS]; Arrays.fill(mask, 0); List<String> tokens = tokens(text); Arrays.fill(mask, 0, Math.min(MAX_TOKENS, tokens.size()), 1); return mask; }
    private List<String> tokens(String text) { List<String> result = new ArrayList<>(); result.add("[CLS]"); result.addAll(Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.!? ]", " ").trim().split("\\s+")).filter(token -> !token.isBlank()).toList()); result.add("[SEP]"); return result; }
    private void normalize(float[] vector) { double norm = 0; for (float value : vector) norm += value * value; norm = Math.sqrt(norm); if (norm > 0) for (int i = 0; i < vector.length; i++) vector[i] /= (float) norm; }
}
