"""Local sentence-transformer embeddings used by the RAG index."""

from __future__ import annotations

from typing import Any

import numpy as np


DEFAULT_EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"


def load_embedding_model(model_name: str = DEFAULT_EMBEDDING_MODEL) -> Any:
    """Load a compact CPU-capable Sentence Transformers model."""
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(model_name, device="cpu")


def embed_texts(model: Any, texts: list[str]) -> np.ndarray:
    """Encode text locally and return unit-length float32 vectors."""
    if not texts:
        return np.empty((0, 0), dtype=np.float32)
    vectors = model.encode(
        texts,
        convert_to_numpy=True,
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    return np.asarray(vectors, dtype=np.float32)


def cosine_similarities(query_vector: np.ndarray, matrix: np.ndarray) -> np.ndarray:
    """Compute cosine similarity, including for vectors not already normalized."""
    matrix = np.asarray(matrix, dtype=np.float32)
    query_vector = np.asarray(query_vector, dtype=np.float32).reshape(-1)
    if matrix.ndim != 2 or matrix.shape[1] != query_vector.size:
        raise ValueError("Query and document embeddings have incompatible dimensions")
    query_norm = np.linalg.norm(query_vector)
    row_norms = np.linalg.norm(matrix, axis=1)
    denominator = row_norms * query_norm
    return np.divide(
        matrix @ query_vector,
        denominator,
        out=np.zeros(matrix.shape[0], dtype=np.float32),
        where=denominator > 0,
    )