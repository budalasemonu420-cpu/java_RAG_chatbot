package com.ragworkshop;

import com.ragworkshop.service.TextCleaningService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TextCleaningServiceTest {
    @Test void normalizesWhitespaceAndControlCharacters() { assertEquals("Hello World\n\nA document.", new TextCleaningService().clean("Hello   World\r\n\r\n\u0000A   document.")); }
}
