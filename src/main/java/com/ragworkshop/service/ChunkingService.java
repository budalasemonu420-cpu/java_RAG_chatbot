package com.ragworkshop.service;

import com.ragworkshop.config.RagConfig;
import com.ragworkshop.model.DocumentChunk;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {
    private final RagConfig config;
    public ChunkingService(RagConfig config) { this.config = config; }
    public List<DocumentChunk> chunk(String sourceFile, Integer pageNumber, String text) {
        return chunk(sourceFile, pageNumber, text, config.getChunkSize(), config.getChunkOverlap());
    }
    public List<DocumentChunk> chunk(String sourceFile, Integer pageNumber, String text, int size, int overlap) {
        if (size <= 0 || overlap < 0 || overlap >= size) throw new IllegalArgumentException("chunkSize must be positive and overlap must be smaller than chunkSize");
        List<DocumentChunk> result = new ArrayList<>();
        int start = 0, number = 1;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start + size / 2) end = boundary;
            }
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) result.add(new DocumentChunk(sourceFile + "-chunk-" + String.format("%03d", number++), sourceFile, pageNumber, part, null));
            if (end >= text.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }
}
