package com.ragworkshop;

import com.ragworkshop.model.DocumentChunk;
import com.ragworkshop.repository.InMemoryVectorStore;
import com.ragworkshop.service.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RetrievalServiceTest {
    @Test void sortsAndAppliesThreshold() { InMemoryVectorStore store = new InMemoryVectorStore(); store.addChunks(List.of(new DocumentChunk("low","a.pdf",1,"low",new float[]{1,0}),new DocumentChunk("high","a.pdf",2,"high",new float[]{1,1}))); EmbeddingService embedding = new EmbeddingService(){public float[] generateEmbedding(String text){return new float[]{1,1};}public List<float[]> generateEmbeddings(List<String> text){return List.of();}}; var result = new RetrievalService(store, embedding, new SimilarityService()).retrieve("question", 1, .5); assertEquals(1, result.size()); assertEquals("high", result.get(0).chunkId()); }

    @Test void skipsNearDuplicateChunksAndBackfillsTopK() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.addChunks(List.of(
                new DocumentChunk("first", "a.pdf", 1, "alpha beta gamma delta epsilon zeta eta theta iota kappa", new float[]{1, 0}),
                new DocumentChunk("duplicate", "a.pdf", 2, "kappa iota theta eta zeta epsilon delta gamma beta alpha", new float[]{1, 0}),
                new DocumentChunk("distinct", "a.pdf", 3, "history differs with unrelated vocabulary", new float[]{1, 0})));
        EmbeddingService embedding = new EmbeddingService(){public float[] generateEmbedding(String text){return new float[]{1,0};}public List<float[]> generateEmbeddings(List<String> text){return List.of();}};

        var result = new RetrievalService(store, embedding, new SimilarityService()).retrieve("question", 2, 0.0);

        assertEquals(List.of("first", "distinct"), result.stream().map(chunk -> chunk.chunkId()).toList());
    }
}
