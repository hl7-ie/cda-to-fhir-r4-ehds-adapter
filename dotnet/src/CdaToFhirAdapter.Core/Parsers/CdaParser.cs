using System.Xml;
using System.Xml.Linq;
using CdaToFhirAdapter.Core.Exceptions;

namespace CdaToFhirAdapter.Core.Parsers;

/// <summary>
/// Parses HL7 CDA R2 XML documents into an <see cref="XDocument"/> and exposes
/// XPath-style helper methods for field extraction.
/// </summary>
/// <remarks>
/// The parser is hardened against XXE attacks by using
/// <see cref="XmlResolver"/>-disabled settings.
/// </remarks>
public sealed class CdaParser
{
    private static readonly XNamespace Hl7Ns = "urn:hl7-org:v3";

    /// <summary>
    /// Parses the supplied CDA XML string into an <see cref="XDocument"/>.
    /// </summary>
    /// <param name="cdaXml">CDA document as an XML string.</param>
    /// <returns>The parsed <see cref="XDocument"/>.</returns>
    /// <exception cref="CdaParseException">Thrown when the XML is null, blank, or malformed.</exception>
    public XDocument Parse(string cdaXml)
    {
        if (string.IsNullOrWhiteSpace(cdaXml))
            throw new CdaParseException("CDA XML must not be null or blank.");

        try
        {
            var settings = new XmlReaderSettings
            {
                DtdProcessing = DtdProcessing.Prohibit,
                XmlResolver = null,
                MaxCharactersFromEntities = 0
            };

            using var reader = XmlReader.Create(new StringReader(cdaXml), settings);
            return XDocument.Load(reader);
        }
        catch (XmlException ex)
        {
            throw new CdaParseException($"Failed to parse CDA XML: {ex.Message}", ex);
        }
    }

    /// <summary>Returns the first matching element value, or an empty string.</summary>
    public string GetElementValue(XDocument doc, params string[] localNames)
    {
        XElement? current = doc.Root;
        foreach (var name in localNames)
        {
            current = current?.Element(Hl7Ns + name);
            if (current is null) return string.Empty;
        }
        return current?.Value.Trim() ?? string.Empty;
    }

    /// <summary>Returns the first matching element, or null.</summary>
    public XElement? GetElement(XDocument doc, params string[] localNames)
    {
        XElement? current = doc.Root;
        foreach (var name in localNames)
        {
            current = current?.Element(Hl7Ns + name);
            if (current is null) return null;
        }
        return current;
    }

    /// <summary>Returns a named attribute value from the first matching element, or empty string.</summary>
    public string GetAttribute(XDocument doc, string attributeName, params string[] localNames)
    {
        var element = GetElement(doc, localNames);
        return element?.Attribute(attributeName)?.Value.Trim() ?? string.Empty;
    }

    /// <summary>Returns all direct children with a given local name under a path.</summary>
    public IEnumerable<XElement> GetElements(XDocument doc, params string[] localNames)
    {
        XElement? current = doc.Root;
        for (int i = 0; i < localNames.Length - 1; i++)
        {
            current = current?.Element(Hl7Ns + localNames[i]);
            if (current is null) return Enumerable.Empty<XElement>();
        }
        return current?.Elements(Hl7Ns + localNames[^1]) ?? Enumerable.Empty<XElement>();
    }
}
