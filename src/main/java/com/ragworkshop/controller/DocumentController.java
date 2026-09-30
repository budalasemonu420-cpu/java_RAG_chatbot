package com.ragworkshop.controller;

import com.ragworkshop.model.DocumentInfo;
import com.ragworkshop.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) public Map<String, Object> upload(@RequestPart("file") MultipartFile file) { DocumentInfo info = service.upload(file); return Map.of("success", true, "fileName", info.fileName(), "pages", info.pages(), "characters", info.characters(), "chunks", info.chunks()); }
    @GetMapping public Collection<DocumentInfo> list() { return service.list(); }
    @DeleteMapping public Map<String, Object> clear() { service.clear(); return Map.of("success", true); }
}
