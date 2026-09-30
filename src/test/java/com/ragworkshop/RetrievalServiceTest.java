package com.ragworkshop;

import com.ragworkshop.model.DocumentChunk;
import com.ragworkshop.repository.InMemoryVectorStore;
import com.ragworkshop.service.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RetrievalServiceTest {
    @Test void sortsAndAppliesThreshold() { InMemoryVectorStore store = new InMemoryVectorStore(); store.addChunks(List.of(new DocumentChunk("low","a.pdf",1,"low",new float[]{1,0}),new DocumentChunk("high","a.pdf",2,"high",new float[]{1,1}))); EmbeddingService embedding = new EmbeddingService(){public float[] generateEmbedding(String text){return new float[]{1,1};}public List<float[]> generateEmbeddings(List<String> text){return List.of();}}; var result = new RetrievalService(store, embedding, new SimilarityService()).retrieve("question", 1, .5); assertEquals(1, result.size()); assertEquals("high", result.get(0).chunkId()); }
}
