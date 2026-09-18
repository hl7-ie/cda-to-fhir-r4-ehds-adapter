package ie.hl7.cda.fhir.parser;

import ie.hl7.cda.fhir.exception.CdaParseException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

/**
 * Parses HL7 CDA R2 XML documents into a DOM {@link Document} and exposes
 * XPath-based accessors for field extraction.
 *
 * <p>The parser is secured against XXE attacks by disabling external entity
 * and DTD processing.</p>
 */
@Component
public class CdaParser {

    /** HL7v3 / CDA namespace URI. */
    public static final String HL7_NS = "urn:hl7-org:v3";

    /** Namespace prefix used in XPath expressions. */
    private static final String NS_PREFIX = "cda";

    /**
     * Parses the supplied CDA XML string into a DOM Document.
     *
     * @param cdaXml the CDA document as an XML string
     * @return the parsed DOM Document
     * @throws CdaParseException if the XML is malformed or entity expansion is detected
     */
    public Document parse(final String cdaXml) {
        if (cdaXml == null || cdaXml.isBlank()) {
            throw new CdaParseException("CDA XML must not be null or blank");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable XXE
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(cdaXml)));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new CdaParseException("Failed to parse CDA XML: " + ex.getMessage(), ex);
        }
    }

    /**
     * Evaluates an XPath expression against the given document and returns the
     * text content of the first matching node.
     *
     * @param doc    source DOM document
     * @param xpathExpression XPath expression using {@code cda:} prefix for the HL7 namespace
     * @return trimmed text content, or an empty string if no match
     */
    public String extractText(final Document doc, final String xpathExpression) {
        try {
            XPath xpath = buildXPath();
            Node node = (Node) xpath.evaluate(xpathExpression, doc, XPathConstants.NODE);
            return node == null ? "" : node.getTextContent().trim();
        } catch (XPathExpressionException ex) {
            return "";
        }
    }

    /**
     * Evaluates an XPath expression and returns the attribute value of the
     * first matching node.
     *
     * @param doc             source DOM document
     * @param xpathExpression XPath expression
     * @param attribute       attribute name to read
     * @return attribute value, or an empty string if no match
     */
    public String extractAttribute(final Document doc, final String xpathExpression,
            final String attribute) {
        try {
            XPath xpath = buildXPath();
            Node node = (Node) xpath.evaluate(xpathExpression, doc, XPathConstants.NODE);
            if (node == null) {
                return "";
            }
            Node attr = node.getAttributes().getNamedItem(attribute);
            return attr == null ? "" : attr.getNodeValue().trim();
        } catch (XPathExpressionException ex) {
            return "";
        }
    }

    /**
     * Returns a NodeList for the given XPath expression.
     *
     * @param doc             source DOM document
     * @param xpathExpression XPath expression
     * @return matching NodeList (may be empty)
     */
    public NodeList extractNodes(final Document doc, final String xpathExpression) {
        try {
            XPath xpath = buildXPath();
            return (NodeList) xpath.evaluate(xpathExpression, doc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            return new EmptyNodeList();
        }
    }

    private XPath buildXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new CdaNamespaceContext());
        return xpath;
    }

    /**
     * Namespace context that maps the {@code cda} prefix to the HL7v3 namespace.
     */
    private static final class CdaNamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(final String prefix) {
            if (NS_PREFIX.equals(prefix)) {
                return HL7_NS;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(final String namespaceURI) {
            if (HL7_NS.equals(namespaceURI)) {
                return NS_PREFIX;
            }
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(final String namespaceURI) {
            return null;
        }
    }

    /**
     * Empty NodeList implementation used as a safe null-object return value.
     */
    private static final class EmptyNodeList implements NodeList {

        @Override
        public Node item(final int index) {
            return null;
        }

        @Override
        public int getLength() {
            return 0;
        }
    }
}
