package ie.hl7.cda.fhir.parser;

import ie.hl7.cda.fhir.exception.CdaParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CdaParser}.
 */
class CdaParserTest {

    private CdaParser parser;

    private static final String MINIMAL_CDA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ClinicalDocument xmlns=\"urn:hl7-org:v3\">"
            + "<id root=\"1.2.3\" extension=\"DOC001\"/>"
            + "<code code=\"60591-5\" codeSystem=\"2.16.840.1.113883.6.1\""
            + " displayName=\"Patient Summary\"/>"
            + "<title>Test Document</title>"
            + "<effectiveTime value=\"20240115\"/>"
            + "</ClinicalDocument>";

    @BeforeEach
    void setUp() {
        parser = new CdaParser();
    }

    @Test
    @DisplayName("parse() returns a non-null Document for valid XML")
    void parseReturnsDocument() {
        Document doc = parser.parse(MINIMAL_CDA);
        assertNotNull(doc);
    }

    @Test
    @DisplayName("parse() throws CdaParseException for null input")
    void parseThrowsOnNullInput() {
        assertThrows(CdaParseException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("parse() throws CdaParseException for blank input")
    void parseThrowsOnBlankInput() {
        assertThrows(CdaParseException.class, () -> parser.parse("   "));
    }

    @Test
    @DisplayName("parse() throws CdaParseException for malformed XML")
    void parseThrowsOnMalformedXml() {
        assertThrows(CdaParseException.class, () -> parser.parse("<unclosed"));
    }

    @Test
    @DisplayName("extractText() returns the title text")
    void extractTextReturnsTitle() {
        Document doc = parser.parse(MINIMAL_CDA);
        String title = parser.extractText(doc, "/cda:ClinicalDocument/cda:title");
        assertEquals("Test Document", title);
    }

    @Test
    @DisplayName("extractAttribute() returns the code attribute")
    void extractAttributeReturnsCode() {
        Document doc = parser.parse(MINIMAL_CDA);
        String code = parser.extractAttribute(doc, "/cda:ClinicalDocument/cda:code", "code");
        assertEquals("60591-5", code);
    }

    @Test
    @DisplayName("extractAttribute() returns empty string for missing attribute")
    void extractAttributeReturnsEmptyForMissing() {
        Document doc = parser.parse(MINIMAL_CDA);
        String result = parser.extractAttribute(doc, "/cda:ClinicalDocument/cda:code", "nonexistent");
        assertEquals("", result);
    }
}
