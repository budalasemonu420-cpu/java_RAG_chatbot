package com.ragworkshop.model;

import java.util.List;

public record ChatResponse(String answer, String model, List<RetrievedChunk> sources) { }
