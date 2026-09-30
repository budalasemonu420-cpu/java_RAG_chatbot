import unittest

import numpy as np

from core.documents import DocumentChunk
from core.embeddings import cosine_similarities, embed_texts
from core.model_selector import select_local_model
from core.retriever import retrieve_top_k


class RetrievalTests(unittest.TestCase):
    def test_embedding_generation_requests_local_normalization(self):
        class FakeModel:
            normalized = False

            def encode(self, texts, **kwargs):
                self.normalized = kwargs["normalize_embeddings"]
                return np.array([[3.0, 4.0] for _ in texts], dtype=np.float32)

        model = FakeModel()
        vectors = embed_texts(model, ["local text"])
        self.assertTrue(model.normalized)
        np.testing.assert_allclose(vectors, [[3.0, 4.0]])

    def test_cosine_similarity_and_top_k_order(self):
        query = np.array([1.0, 0.0], dtype=np.float32)
        matrix = np.array([[1.0, 0.0], [0.0, 1.0], [-1.0, 0.0]])
        np.testing.assert_allclose(cosine_similarities(query, matrix), [1.0, 0.0, -1.0])
        chunks = [DocumentChunk(str(i), "notes.pdf", i + 1, f"topic {i} text") for i in range(3)]
        result = retrieve_top_k(query, chunks, matrix, top_k=2, min_similarity=0.0)
        self.assertEqual([item.chunk.chunk_id for item in result], ["0", "1"])

    def test_qwen_has_priority_and_llama_is_fallback(self):
        self.assertEqual(
            select_local_model(["llama3.2:3b", "qwen2.5:3b"]).name,
            "qwen2.5:3b",
        )
        self.assertEqual(select_local_model(["llama3.1:8b"]).family, "Llama")
        self.assertIsNone(select_local_model(["mistral:7b"]).name)


if __name__ == "__main__":
    unittest.main()