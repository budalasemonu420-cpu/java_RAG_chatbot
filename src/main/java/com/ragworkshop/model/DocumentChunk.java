package com.ragworkshop.model;

public record DocumentChunk(String chunkId, String sourceFile, Integer pageNumber, String text, float[] embedding) { }
