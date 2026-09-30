package com.ragworkshop.repository;

import com.ragworkshop.model.DocumentChunk;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryVectorStore {
    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();
    public void addChunk(DocumentChunk chunk) { chunks.add(chunk); }
    public void addChunks(List<DocumentChunk> newChunks) { chunks.addAll(newChunks); }
    public List<DocumentChunk> getAllChunks() { return new ArrayList<>(chunks); }
    public void clear() { chunks.clear(); }
    public int size() { return chunks.size(); }
    public void removeSource(String source) { chunks.removeIf(chunk -> chunk.sourceFile().equals(source)); }
}
