"""Transparent Top-K retrieval using cosine similarity."""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from core.documents import DocumentChunk
from core.embeddings import cosine_similarities


@dataclass(frozen=True)
class RetrievedChunk:
    chunk: DocumentChunk
    score: float


def retrieve_top_k(
    query_embedding: np.ndarray,
    chunks: list[DocumentChunk],
    embeddings: np.ndarray,
    top_k: int = 4,
    min_similarity: float = 0.15,
    redundancy_threshold: float = 0.9,
) -> list[RetrievedChunk]:
    if top_k < 1:
        raise ValueError("top_k must be at least 1")
    if not -1 <= min_similarity <= 1:
        raise ValueError("min_similarity must be between -1 and 1")
    if len(chunks) != len(embeddings):
        raise ValueError("Every chunk must have exactly one embedding")
    if not chunks:
        return []

    scores = cosine_similarities(query_embedding, embeddings)
    ranked_indices = np.argsort(scores)[::-1]
    results: list[RetrievedChunk] = []
    seen_texts: list[set[str]] = []
    for index in ranked_indices:
        score = float(scores[index])
        if score < min_similarity:
            break
        words = set(chunks[int(index)].text.lower().split())
        if words and any(
            len(words & previous) / max(1, len(words | previous)) >= redundancy_threshold
            for previous in seen_texts
        ):
            continue
        results.append(RetrievedChunk(chunks[int(index)], score))
        seen_texts.append(words)
        if len(results) == top_k:
            break
    return results