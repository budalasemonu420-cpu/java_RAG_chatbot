import unittest
from unittest.mock import patch

import numpy as np

from core.documents import DocumentChunk
from core.rag_pipeline import answer_question


class FakeEmbeddingModel:
    def encode(self, texts, **kwargs):
        return np.array([[0.0, 1.0] for _ in texts], dtype=np.float32)


class RAGPipelineTests(unittest.TestCase):
    def setUp(self):
        self.chunks = [DocumentChunk("course:1:1", "course.pdf", 1, "Eligibility requires enrollment.")]
        self.embeddings = np.array([[1.0, 0.0]], dtype=np.float32)

    @patch("core.rag_pipeline.generate_answer")
    def test_irrelevant_retrieval_does_not_call_ollama(self, generate):
        result = answer_question(
            "What is the Wi-Fi password?",
            self.chunks,
            self.embeddings,
            FakeEmbeddingModel(),
            "qwen2.5:3b",
            min_similarity=0.5,
        )
        self.assertFalse(result.retrieved)
        self.assertIsNone(result.model_name)
        self.assertIn("could not find relevant information", result.answer)
        generate.assert_not_called()

    @patch("core.rag_pipeline.generate_answer", return_value="The policy requires enrollment.")
    def test_retrieved_context_and_question_reach_local_model(self, generate):
        result = answer_question(
            "What does eligibility require?",
            self.chunks,
            self.embeddings,
            FakeEmbeddingModel(),
            "qwen2.5:3b",
            min_similarity=0.0,
        )
        self.assertEqual(result.answer, "The policy requires enrollment.")
        self.assertEqual(result.model_name, "qwen2.5:3b")
        args = generate.call_args.args
        self.assertIn("Eligibility requires enrollment.", args[2])
        self.assertIn("What does eligibility require?", args[2])


if __name__ == "__main__":
    unittest.main()