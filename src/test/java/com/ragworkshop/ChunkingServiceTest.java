package com.ragworkshop;

import com.ragworkshop.config.RagConfig;
import com.ragworkshop.service.ChunkingService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {
    @Test void createsBoundedOverlappingChunksWithMetadata() { RagConfig config = new RagConfig(); ChunkingService service = new ChunkingService(config); var chunks = service.chunk("guide.pdf", 2, "one two three four five six seven eight nine ten", 20, 5); assertTrue(chunks.size() > 1); assertEquals("guide.pdf", chunks.get(0).sourceFile()); assertEquals(2, chunks.get(0).pageNumber()); assertTrue(chunks.get(0).text().length() <= 20); }
}
