using CdaToFhirAdapter.Core.Exceptions;
using CdaToFhirAdapter.Core.Parsers;
using FluentAssertions;
using Xunit;

namespace CdaToFhirAdapter.Tests.Parsers;

/// <summary>Unit tests for <see cref="CdaParser"/>.</summary>
public sealed class CdaParserTests
{
    private readonly CdaParser _parser = new();

    private const string MinimalCda = """
        <?xml version="1.0" encoding="UTF-8"?>
        <ClinicalDocument xmlns="urn:hl7-org:v3">
          <id root="1.2.3" extension="DOC001"/>
          <code code="60591-5" codeSystem="2.16.840.1.113883.6.1" displayName="Patient Summary"/>
          <title>Test Document</title>
          <effectiveTime value="20240115"/>
        </ClinicalDocument>
        """;

    [Fact]
    public void Parse_ValidXml_ReturnsDocument()
    {
        var doc = _parser.Parse(MinimalCda);
        doc.Should().NotBeNull();
        doc.Root.Should().NotBeNull();
    }

    [Fact]
    public void Parse_NullInput_ThrowsCdaParseException()
    {
        var act = () => _parser.Parse(null!);
        act.Should().Throw<CdaParseException>();
    }

    [Fact]
    public void Parse_BlankInput_ThrowsCdaParseException()
    {
        var act = () => _parser.Parse("   ");
        act.Should().Throw<CdaParseException>();
    }

    [Fact]
    public void Parse_MalformedXml_ThrowsCdaParseException()
    {
        var act = () => _parser.Parse("<unclosed");
        act.Should().Throw<CdaParseException>();
    }

    [Fact]
    public void GetElementValue_Title_ReturnsTitleText()
    {
        var doc = _parser.Parse(MinimalCda);
        var title = _parser.GetElementValue(doc, "title");
        title.Should().Be("Test Document");
    }

    [Fact]
    public void GetAttribute_CodeElement_ReturnsCodeValue()
    {
        var doc = _parser.Parse(MinimalCda);
        var code = _parser.GetAttribute(doc, "code", "code");
        code.Should().Be("60591-5");
    }

    [Fact]
    public void GetAttribute_MissingAttribute_ReturnsEmptyString()
    {
        var doc = _parser.Parse(MinimalCda);
        var result = _parser.GetAttribute(doc, "nonexistent", "code");
        result.Should().BeEmpty();
    }
}
