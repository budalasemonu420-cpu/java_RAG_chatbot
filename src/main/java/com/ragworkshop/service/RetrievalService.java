package com.ragworkshop.service;

import com.ragworkshop.model.RetrievedChunk;
import com.ragworkshop.repository.InMemoryVectorStore;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RetrievalService {
    private static final double REDUNDANCY_THRESHOLD = 0.9;
    private final InMemoryVectorStore store;
    private final EmbeddingService embeddings;
    private final SimilarityService similarity;
    public RetrievalService(InMemoryVectorStore store, EmbeddingService embeddings, SimilarityService similarity) { this.store = store; this.embeddings = embeddings; this.similarity = similarity; }
    public List<RetrievedChunk> retrieve(String question, int topK, double threshold) {
        float[] query = embeddings.generateEmbedding(question);
        List<RetrievedChunk> ranked = store.getAllChunks().stream().map(chunk -> new RetrievedChunk(chunk.chunkId(), chunk.sourceFile(), chunk.pageNumber(), similarity.cosineSimilarity(query, chunk.embedding()), chunk.text()))
            .filter(chunk -> chunk.similarityScore() >= threshold).sorted(Comparator.comparingDouble((RetrievedChunk chunk) -> chunk.similarityScore()).reversed()).toList();
        List<RetrievedChunk> results = new ArrayList<>();
        List<Set<String>> seenWords = new ArrayList<>();
        for (RetrievedChunk chunk : ranked) {
            Set<String> words = new HashSet<>(Arrays.asList(chunk.text().toLowerCase(Locale.ROOT).split("\\s+")));
            boolean redundant = !words.isEmpty() && seenWords.stream().anyMatch(previous -> jaccard(words, previous) >= REDUNDANCY_THRESHOLD);
            if (redundant) continue;
            results.add(chunk);
            seenWords.add(words);
            if (results.size() == topK) break;
        }
        return List.copyOf(results);
    }

    private double jaccard(Set<String> left, Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }
}
