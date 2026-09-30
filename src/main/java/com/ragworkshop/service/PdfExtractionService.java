package com.ragworkshop.service;

import com.ragworkshop.exception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractionService {
    public ExtractionResult extract(MultipartFile file) {
        try (var document = Loader.loadPDF(file.getBytes())) {
            List<PageText> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                pages.add(new PageText(page, stripper.getText(document)));
            }
            if (pages.stream().allMatch(page -> page.text().isBlank())) throw new DocumentProcessingException("The PDF contains no selectable text. It may be image-only and need OCR.");
            return new ExtractionResult(pages, document.getNumberOfPages());
        } catch (DocumentProcessingException exception) { throw exception; }
        catch (Exception exception) { throw new DocumentProcessingException("Unable to extract text from the uploaded PDF.", exception); }
    }
    public record PageText(Integer pageNumber, String text) { }
    public record ExtractionResult(List<PageText> pages, int pageCount) { }
}
