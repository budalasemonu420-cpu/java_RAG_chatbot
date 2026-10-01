package com.ragworkshop.service;

import com.ragworkshop.exception.DocumentProcessingException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocxExtractionService {
    public ExtractionResult extract(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<String> parts = new ArrayList<>(document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText())
                .filter(value -> value != null && !value.isBlank())
                .toList());
            document.getTables().forEach(table -> table.getRows().forEach(row -> {
            String rowText = String.join(" | ", row.getTableCells().stream()
                .map(cell -> cell.getText())
                .filter(value -> value != null && !value.isBlank())
                .toList());
            if (!rowText.isBlank()) parts.add(rowText);
            }));
            String text = String.join("\n", parts);
            if (text.isBlank()) throw new DocumentProcessingException("The DOCX contains no extractable text.");
            return new ExtractionResult(text);
        } catch (DocumentProcessingException exception) { throw exception; }
        catch (Exception exception) { throw new DocumentProcessingException("Unable to extract text from the uploaded DOCX.", exception); }
    }
    public record ExtractionResult(String text) { }
}
