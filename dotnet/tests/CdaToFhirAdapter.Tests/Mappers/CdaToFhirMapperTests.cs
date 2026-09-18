using CdaToFhirAdapter.Core.Mappers;
using CdaToFhirAdapter.Core.Parsers;
using FluentAssertions;
using Hl7.Fhir.Model;
using Xunit;

namespace CdaToFhirAdapter.Tests.Mappers;

/// <summary>Unit tests for <see cref="CdaToFhirMapper"/>.</summary>
public sealed class CdaToFhirMapperTests
{
    private readonly CdaToFhirMapper _mapper = new(new CdaParser());

    private static string LoadSampleCda()
    {
        var path = Path.Combine(
            AppDomain.CurrentDomain.BaseDirectory,
            "Resources",
            "sample-patient-summary.xml");
        return File.ReadAllText(path);
    }

    [Fact]
    public void Map_ValidCda_ReturnsDocumentBundle()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        bundle.Should().NotBeNull();
        bundle.Type.Should().Be(Bundle.BundleType.Document);
    }

    [Fact]
    public void Map_ValidCda_BundleContainsFourEntries()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        bundle.Entry.Should().HaveCount(4);
    }

    [Fact]
    public void Map_ValidCda_CompositionHasCorrectLoincCode()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var composition = bundle.Entry[0].Resource as Composition;
        composition.Should().NotBeNull();
        composition!.Type.Coding.Should().ContainSingle(c => c.Code == "60591-5");
    }

    [Fact]
    public void Map_ValidCda_PatientHasCorrectName()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var patient = bundle.Entry[1].Resource as Patient;
        patient.Should().NotBeNull();
        patient!.Name.Should().NotBeEmpty();
        patient.Name[0].Family.Should().Be("Doe");
        patient.Name[0].Given.Should().Contain("John");
    }

    [Fact]
    public void Map_ValidCda_PatientGenderIsMale()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var patient = bundle.Entry[1].Resource as Patient;
        patient!.Gender.Should().Be(AdministrativeGender.Male);
    }

    [Fact]
    public void Map_ValidCda_PractitionerHasCorrectName()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var practitioner = bundle.Entry[2].Resource as Practitioner;
        practitioner.Should().NotBeNull();
        practitioner!.Name.Should().NotBeEmpty();
        practitioner.Name[0].Family.Should().Be("Smith");
    }

    [Fact]
    public void Map_ValidCda_CompositionHasTwoSections()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var composition = bundle.Entry[0].Resource as Composition;
        composition!.Section.Should().HaveCount(2);
        composition.Section[0].Title.Should().Be("Active Problems");
    }

    [Fact]
    public void Map_ValidCda_BundleIdentifierIsSet()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        bundle.Identifier.Should().NotBeNull();
        bundle.Identifier.Value.Should().Be("PS-2024-001");
    }

    [Fact]
    public void Map_ValidCda_OrganizationHasName()
    {
        var bundle = _mapper.Map(LoadSampleCda());

        var org = bundle.Entry[3].Resource as Organization;
        org.Should().NotBeNull();
        org!.Name.Should().Be("Health Service Executive");
    }
}
