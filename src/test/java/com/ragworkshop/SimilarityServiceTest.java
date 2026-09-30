package com.ragworkshop;

import com.ragworkshop.service.SimilarityService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityServiceTest {
    private final SimilarityService service = new SimilarityService();
    @Test void identicalVectorsScoreOne() { assertEquals(1.0, service.cosineSimilarity(new float[]{1,2}, new float[]{1,2}), 0.0001); }
    @Test void zeroVectorScoresZero() { assertEquals(0, service.cosineSimilarity(new float[]{0,0}, new float[]{1,2})); }
    @Test void orthogonalVectorsScoreZero() { assertEquals(0, service.cosineSimilarity(new float[]{1,0}, new float[]{0,1})); }
}
