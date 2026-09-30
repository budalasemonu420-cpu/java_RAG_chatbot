import unittest
from pathlib import Path

from streamlit.testing.v1 import AppTest


class StreamlitAppTests(unittest.TestCase):
    def test_app_renders_without_ollama_or_uploaded_files(self):
        app_path = Path(__file__).resolve().parents[1] / "app.py"
        app = AppTest.from_file(str(app_path), default_timeout=15).run()
        self.assertFalse(app.exception)
        self.assertEqual(app.title[0].value, "Fieldnotes")
        self.assertEqual(len(app.metric), 4)
        self.assertEqual(app.metric[0].value, "0")


if __name__ == "__main__":
    unittest.main()