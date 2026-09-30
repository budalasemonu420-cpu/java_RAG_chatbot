package com.ragworkshop.service;

import com.ragworkshop.model.DocumentChunk;
import com.ragworkshop.model.RetrievedChunk;
import com.ragworkshop.repository.InMemoryVectorStore;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class RetrievalService {
    private final InMemoryVectorStore store;
    private final EmbeddingService embeddings;
    private final SimilarityService similarity;
    public RetrievalService(InMemoryVectorStore store, EmbeddingService embeddings, SimilarityService similarity) { this.store = store; this.embeddings = embeddings; this.similarity = similarity; }
    public List<RetrievedChunk> retrieve(String question, int topK, double threshold) {
        float[] query = embeddings.generateEmbedding(question);
        return store.getAllChunks().stream().map(chunk -> new RetrievedChunk(chunk.chunkId(), chunk.sourceFile(), chunk.pageNumber(), similarity.cosineSimilarity(query, chunk.embedding()), chunk.text()))
                .filter(chunk -> chunk.similarityScore() >= threshold).sorted(Comparator.comparingDouble(RetrievedChunk::similarityScore).reversed()).limit(topK).toList();
    }
}
