package com.ragworkshop.service;

import org.springframework.stereotype.Service;

@Service
public class TextCleaningService {
    public String clean(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("[ \\t]*\\r?\\n[ \\t]*", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
