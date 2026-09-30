package com.ragworkshop.model;

public record RetrievedChunk(String chunkId, String sourceFile, Integer pageNumber, double similarityScore, String text) { }
