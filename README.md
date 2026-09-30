# Chat with Documents - Java Full Stack Local RAG

A beginner-friendly Spring Boot workshop application that uploads PDF/DOCX files, extracts and cleans their text, creates overlapping chunks, embeds them locally, retrieves the most similar chunks with cosine similarity, and asks a locally running Ollama model for a grounded answer.

No API key, paid service, Python, Node.js backend, or cloud LLM is required.

## Pipeline

```text
PDF / DOCX -> Extract -> Clean -> Chunk -> Embed -> Cosine Similarity
          -> Top-K Retrieval -> Context -> Ollama -> Qwen/Llama -> Answer
```

RAG does not make an LLM read an entire PDF automatically. This application first finds relevant chunks and sends only those chunks as context for each question. Document text is untrusted data: it is placed after the system rules, so instructions inside an upload are not treated as application instructions.

## Technology

- Java 21, Spring Boot 3.5, Maven
- Apache PDFBox 3 for selectable PDF text and page numbers
- Apache POI 5 for DOCX paragraphs
- ONNX Runtime for local `all-MiniLM-L6-v2` sentence embeddings
- Java `HttpClient` for Ollama at `http://localhost:11434`
- HTML, CSS, and browser Fetch API served from Spring Boot
- JUnit 5 and Mockito/Spring test dependencies

### Embedding model setup

The embedding adapter is real ONNX inference; it never returns random or hard-coded vectors. Place these files in `D:\RAG\models\all-MiniLM-L6-v2\`:

```text
model.onnx
vocab.txt
```

Use a trusted export of the Hugging Face `sentence-transformers/all-MiniLM-L6-v2` model. Convert/download the model files explicitly before the first upload; the application never silently downloads embedding or Ollama models. A CPU ONNX export generally needs a few hundred MB of disk/RAM and works without a GPU. If the files are absent, indexing fails with a setup message rather than producing fake embeddings.

## Windows setup

Install Java 21, Maven, and Ollama, then verify:

```powershell
java -version
mvn -version
ollama --version
ollama list
```

Start Ollama and explicitly install one supported local model, for example:

```powershell
ollama pull qwen2.5:3b
# or
ollama pull llama3.2:3b
```

The application checks installed tags dynamically. It chooses a model whose name contains `qwen` first, then one containing `llama`. It never downloads an LLM model. If no supported model is installed, the UI shows setup guidance.

## Run

```powershell
cd /d D:\RAG
mvn clean install
mvn spring-boot:run
```

Open <http://localhost:8080>. Upload a text-based PDF or DOCX, wait for indexing, and ask a question. Scanned/image-only PDFs need OCR first.

## Configuration

`src/main/resources/application.properties` contains:

```properties
server.port=8080
ollama.base-url=http://localhost:11434
rag.chunk-size=800
rag.chunk-overlap=100
rag.top-k=3
rag.similarity-threshold=0.45
rag.temperature=0.2
rag.embedding-model-path=models/all-MiniLM-L6-v2/model.onnx
rag.tokenizer-path=models/all-MiniLM-L6-v2/vocab.txt
```

Chunk overlap preserves context across boundaries. Cosine similarity is `(A dot B) / (||A|| * ||B||)`. If no chunk reaches the threshold, Ollama is not called and the application returns: `I could not find relevant information in the uploaded documents.`

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/documents/upload` | Upload PDF/DOCX and index it |
| GET | `/api/documents` | List indexed documents |
| DELETE | `/api/documents` | Clear documents and vectors |
| POST | `/api/chat` | Retrieve context and ask Ollama |
| GET | `/api/system/status` | Ollama and selected model status |
| GET | `/api/system/models` | List installed Ollama models |
| GET | `/api/index/status` | Document/chunk/embedding status |
| DELETE | `/api/chat/history` | Clear browser chat state response |

## Architecture and class guide

```text
Browser (HTML/CSS/JS)
        | REST
Spring Boot controllers
        |
DocumentService -> PDFBox / POI -> cleaning -> chunking -> ONNX embeddings -> InMemoryVectorStore
RagPipelineService -> RetrievalService -> SimilarityService -> RagPromptBuilder -> OllamaService
ModelSelectionService: Qwen first, Llama fallback
```

- `DocumentController` receives multipart uploads and document commands.
- `DocumentService` coordinates extraction, cleaning, chunking, embedding, and storage.
- `PdfExtractionService` extracts each PDF page; `DocxExtractionService` extracts paragraphs.
- `TextCleaningService` removes control characters and excess whitespace without removing meaning.
- `ChunkingService` creates configurable overlapping `DocumentChunk` records.
- `OnnxEmbeddingService` tokenizes locally and mean-pools real MiniLM ONNX output.
- `InMemoryVectorStore` keeps chunks, metadata, and vectors in memory for teaching.
- `RetrievalService` embeds the question, calculates scores, filters, sorts, and limits Top-K.
- `RagPromptBuilder` separates system instructions, document context, and the question.
- `OllamaService` calls `/api/tags` and `/api/generate` with Java `HttpClient`.
- `RagPipelineService` prevents generation when retrieval has no qualifying result.
- `GlobalExceptionHandler` returns clean JSON instead of stack traces.

## Workshop demo

1. Start Ollama and run `ollama list`.
2. Start the Spring Boot app from `D:\RAG`.
3. Upload a syllabus PDF.
4. Show pages, characters, chunks, and embeddings ready.
5. Ask `What are the eligibility requirements?`.
6. Expand Retrieved Sources and compare chunk IDs and scores.
7. Ask an unrelated question to demonstrate the no-result guard.

Example questions: `What is the course duration?`, `What are the examination requirements?`, `What documents are required?`, and `What are the important dates?`.

## Tests

```powershell
mvn test
```

Tests cover text cleanup, chunk metadata and overlap, cosine similarity, and thresholded Top-K retrieval. PDF/DOCX integration tests can be added with small workshop fixtures.

## Troubleshooting

- **Ollama not connected:** start the Ollama desktop app or run `ollama serve`.
- **No Qwen/Llama model:** inspect `ollama list`, then explicitly run `ollama pull <tag>`.
- **PDF has no text:** it is likely scanned/image-only; OCR it first.
- **DOCX fails:** upload a valid `.docx`, not a renamed file.
- **Embedding model failed:** confirm `models/all-MiniLM-L6-v2/model.onnx` and `vocab.txt` exist and match the model.
- **Port 8080 is busy:** set `server.port=8081`, then open `http://localhost:8081`.

The old Python files may remain in this folder as historical workshop material; the runnable application described here is the Java/Maven project.
