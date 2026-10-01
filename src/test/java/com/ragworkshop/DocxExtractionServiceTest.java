package com.ragworkshop;

import com.ragworkshop.service.DocxExtractionService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxExtractionServiceTest {
    @Test
    void extractsTextFromTablesWithoutParagraphs() throws Exception {
        byte[] documentBytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var row = document.createTable(1, 2).getRow(0);
            row.getCell(0).setText("Assessment");
            row.getCell(1).setText("Final exam");
            document.write(output);
            documentBytes = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "course.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                documentBytes);

        var result = new DocxExtractionService().extract(file);

        assertTrue(result.text().contains("Assessment | Final exam"));
    }
}