package com.ragworkshop.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String question, Integer topK, Double similarityThreshold, Double temperature) { }
