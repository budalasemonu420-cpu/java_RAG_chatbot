package com.ragworkshop.service;

import com.ragworkshop.exception.DocumentProcessingException;
import com.ragworkshop.model.DocumentChunk;
import com.ragworkshop.model.DocumentInfo;
import com.ragworkshop.repository.InMemoryVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentService {
    private final PdfExtractionService pdf; private final DocxExtractionService docx; private final TextCleaningService cleaner; private final ChunkingService chunker; private final EmbeddingService embeddings; private final InMemoryVectorStore store;
    private final Map<String, DocumentInfo> documents = new ConcurrentHashMap<>();
    public DocumentService(PdfExtractionService pdf, DocxExtractionService docx, TextCleaningService cleaner, ChunkingService chunker, EmbeddingService embeddings, InMemoryVectorStore store) { this.pdf = pdf; this.docx = docx; this.cleaner = cleaner; this.chunker = chunker; this.embeddings = embeddings; this.store = store; }
    public synchronized DocumentInfo upload(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (file.isEmpty() || !(name.toLowerCase(Locale.ROOT).endsWith(".pdf") || name.toLowerCase(Locale.ROOT).endsWith(".docx"))) throw new DocumentProcessingException("Upload a non-empty PDF or DOCX file.");
        store.removeSource(name); List<DocumentChunk> chunks = new ArrayList<>(); int pages = 1; StringBuilder allText = new StringBuilder();
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) { var result = pdf.extract(file); pages = result.pageCount(); for (var page : result.pages()) { String text = cleaner.clean(page.text()); allText.append(text).append('\n'); chunks.addAll(chunker.chunk(name, page.pageNumber(), text)); } }
        else { String text = cleaner.clean(docx.extract(file).text()); allText.append(text); chunks.addAll(chunker.chunk(name, null, text)); }
        List<float[]> vectors = embeddings.generateEmbeddings(chunks.stream().map(DocumentChunk::text).toList());
        List<DocumentChunk> indexed = new ArrayList<>(); for (int i = 0; i < chunks.size(); i++) { DocumentChunk chunk = chunks.get(i); indexed.add(new DocumentChunk(chunk.chunkId(), chunk.sourceFile(), chunk.pageNumber(), chunk.text(), vectors.get(i))); }
        store.addChunks(indexed); DocumentInfo info = new DocumentInfo(name, pages, allText.toString().trim().length(), indexed.size()); documents.put(name, info); return info;
    }
    public Collection<DocumentInfo> list() { return documents.values(); }
    public void clear() { documents.clear(); store.clear(); }
    public int chunkCount() { return store.size(); }
    public int documentCount() { return documents.size(); }
}
