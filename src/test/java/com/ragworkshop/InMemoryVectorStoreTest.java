package com.ragworkshop;

import com.ragworkshop.model.DocumentChunk;
import com.ragworkshop.repository.InMemoryVectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVectorStoreTest {
    @Test
    void addsIndividualAndMultipleChunks() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        DocumentChunk first = chunk("chunk-1", "first.pdf");
        DocumentChunk second = chunk("chunk-2", "second.pdf");
        DocumentChunk third = chunk("chunk-3", "second.pdf");

        store.addChunk(first);
        store.addChunks(List.of(second, third));

        assertEquals(3, store.size());
        assertEquals(List.of(first, second, third), store.getAllChunks());
    }

    @Test
    void returnsSnapshotRatherThanMutableStoreList() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.addChunk(chunk("chunk-1", "first.pdf"));
        List<DocumentChunk> snapshot = store.getAllChunks();

        snapshot.clear();

        assertEquals(1, store.size());
    }

    @Test
    void removesEveryChunkFromSelectedSourceOnly() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.addChunks(List.of(
                chunk("first-1", "first.pdf"),
                chunk("second", "second.pdf"),
                chunk("first-2", "first.pdf")));

        store.removeSource("first.pdf");

        assertEquals(1, store.size());
        assertEquals("second.pdf", store.getAllChunks().get(0).sourceFile());
    }

    @Test
    void clearsAllChunks() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.addChunks(List.of(chunk("first", "first.pdf"), chunk("second", "second.pdf")));

        store.clear();

        assertTrue(store.getAllChunks().isEmpty());
        assertEquals(0, store.size());
    }

    private DocumentChunk chunk(String id, String source) {
        return new DocumentChunk(id, source, 1, "text", new float[]{1.0f, 0.0f});
    }
}