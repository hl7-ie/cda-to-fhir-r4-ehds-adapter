using System.Xml.Linq;
using CdaToFhirAdapter.Core.Exceptions;
using CdaToFhirAdapter.Core.Parsers;
using Hl7.Fhir.Model;

namespace CdaToFhirAdapter.Core.Mappers;

/// <summary>
/// Maps a parsed CDA R2 <see cref="System.Xml.Linq.XDocument"/> to a FHIR R4
/// <see cref="Bundle"/> of type <c>document</c>.
/// </summary>
/// <remarks>
/// Resources created:
/// <list type="bullet">
///   <item><see cref="Composition"/> – from the CDA document header</item>
///   <item><see cref="Patient"/> – from <c>recordTarget/patientRole</c></item>
///   <item><see cref="Practitioner"/> – from the first <c>author</c> element</item>
///   <item><see cref="Organization"/> – from the <c>custodian</c> element</item>
/// </list>
/// </remarks>
public sealed class CdaToFhirMapper
{
    private static readonly XNamespace Hl7Ns = "urn:hl7-org:v3";
    private const string LoincSystem = "http://loinc.org";
    private const string EhdsCompositionProfile =
        "http://hl7.eu/fhir/ehds/StructureDefinition/composition-eu-ehds";

    private readonly CdaParser _parser;

    /// <summary>Constructs the mapper with the required <see cref="CdaParser"/>.</summary>
    public CdaToFhirMapper(CdaParser parser)
    {
        _parser = parser;
    }

    /// <summary>
    /// Converts a CDA XML string to a FHIR R4 <see cref="Bundle"/>.
    /// </summary>
    /// <param name="cdaXml">Raw CDA XML string.</param>
    /// <returns>A FHIR document <see cref="Bundle"/>.</returns>
    /// <exception cref="FhirMappingException">Thrown when the mapping fails.</exception>
    public Bundle Map(string cdaXml)
    {
        System.Xml.Linq.XDocument doc;
        try
        {
            doc = _parser.Parse(cdaXml);
        }
        catch (Exception ex)
        {
            throw new FhirMappingException($"CDA parsing failed: {ex.Message}", ex);
        }

        var patient = BuildPatient(doc);
        var practitioner = BuildPractitioner(doc);
        var organization = BuildOrganization(doc);
        var composition = BuildComposition(doc, patient, practitioner, organization);

        var bundle = new Bundle
        {
            Id = Guid.NewGuid().ToString(),
            Type = Bundle.BundleType.Document,
            Timestamp = DateTimeOffset.UtcNow
        };

        // Bundle identifier from ClinicalDocument/id
        var docRoot = _parser.GetAttribute(doc, "root", "id");
        var docExt = _parser.GetAttribute(doc, "extension", "id");
        if (!string.IsNullOrEmpty(docRoot))
        {
            bundle.Identifier = new Identifier
            {
                System = $"urn:oid:{docRoot}",
                Value = string.IsNullOrEmpty(docExt) ? docRoot : docExt
            };
        }

        bundle.Entry.Add(new Bundle.EntryComponent
        {
            FullUrl = $"urn:uuid:{composition.Id}",
            Resource = composition
        });
        bundle.Entry.Add(new Bundle.EntryComponent
        {
            FullUrl = $"urn:uuid:{patient.Id}",
            Resource = patient
        });
        bundle.Entry.Add(new Bundle.EntryComponent
        {
            FullUrl = $"urn:uuid:{practitioner.Id}",
            Resource = practitioner
        });
        bundle.Entry.Add(new Bundle.EntryComponent
        {
            FullUrl = $"urn:uuid:{organization.Id}",
            Resource = organization
        });

        return bundle;
    }

    private Patient BuildPatient(System.Xml.Linq.XDocument doc)
    {
        var patient = new Patient { Id = Guid.NewGuid().ToString() };

        var roleEl = doc.Root
            ?.Element(Hl7Ns + "recordTarget")
            ?.Element(Hl7Ns + "patientRole");

        // Identifier
        var idEl = roleEl?.Element(Hl7Ns + "id");
        var idRoot = idEl?.Attribute("root")?.Value;
        var idExt = idEl?.Attribute("extension")?.Value;
        if (!string.IsNullOrEmpty(idRoot))
        {
            patient.Identifier.Add(new Identifier
            {
                System = $"urn:oid:{idRoot}",
                Value = string.IsNullOrEmpty(idExt) ? idRoot : idExt
            });
        }

        // Name
        var patientEl = roleEl?.Element(Hl7Ns + "patient");
        var nameEl = patientEl?.Element(Hl7Ns + "name");
        var given = nameEl?.Element(Hl7Ns + "given")?.Value.Trim();
        var family = nameEl?.Element(Hl7Ns + "family")?.Value.Trim();
        if (!string.IsNullOrEmpty(given) || !string.IsNullOrEmpty(family))
        {
            var humanName = new HumanName { Use = HumanName.NameUse.Official };
            if (!string.IsNullOrEmpty(family)) humanName.Family = family;
            if (!string.IsNullOrEmpty(given)) humanName.Given = [given];
            patient.Name.Add(humanName);
        }

        // Gender
        var genderCode = patientEl
            ?.Element(Hl7Ns + "administrativeGenderCode")
            ?.Attribute("code")?.Value;
        patient.Gender = MapGender(genderCode);

        // Birth date
        var birthValue = patientEl
            ?.Element(Hl7Ns + "birthTime")
            ?.Attribute("value")?.Value;
        var birthDate = ParseCdaDate(birthValue);
        if (birthDate.HasValue)
            patient.BirthDateElement = new Date(birthDate.Value.Year, birthDate.Value.Month, birthDate.Value.Day);

        // Address
        var addrEl = roleEl?.Element(Hl7Ns + "addr");
        var street = addrEl?.Element(Hl7Ns + "streetAddressLine")?.Value.Trim();
        var city = addrEl?.Element(Hl7Ns + "city")?.Value.Trim();
        var country = addrEl?.Element(Hl7Ns + "country")?.Value.Trim();
        if (!string.IsNullOrEmpty(street) || !string.IsNullOrEmpty(city))
        {
            var address = new Address();
            if (!string.IsNullOrEmpty(street)) address.Line = [street];
            if (!string.IsNullOrEmpty(city)) address.City = city;
            if (!string.IsNullOrEmpty(country)) address.Country = country;
            patient.Address.Add(address);
        }

        // Telecom
        var telecomValue = roleEl?.Element(Hl7Ns + "telecom")?.Attribute("value")?.Value;
        if (!string.IsNullOrEmpty(telecomValue))
        {
            patient.Telecom.Add(new ContactPoint
            {
                System = ContactPoint.ContactPointSystem.Phone,
                Value = telecomValue.Replace("tel:", "", StringComparison.OrdinalIgnoreCase)
            });
        }

        return patient;
    }

    private Practitioner BuildPractitioner(System.Xml.Linq.XDocument doc)
    {
        var practitioner = new Practitioner { Id = Guid.NewGuid().ToString() };

        var authorEl = doc.Root?.Element(Hl7Ns + "author");
        var assignedAuthor = authorEl?.Element(Hl7Ns + "assignedAuthor");
        var idEl = assignedAuthor?.Element(Hl7Ns + "id");
        var idRoot = idEl?.Attribute("root")?.Value;
        var idExt = idEl?.Attribute("extension")?.Value;

        if (!string.IsNullOrEmpty(idRoot))
        {
            practitioner.Identifier.Add(new Identifier
            {
                System = $"urn:oid:{idRoot}",
                Value = string.IsNullOrEmpty(idExt) ? idRoot : idExt
            });
        }

        var personEl = assignedAuthor?.Element(Hl7Ns + "assignedPerson");
        var nameEl = personEl?.Element(Hl7Ns + "name");
        var given = nameEl?.Element(Hl7Ns + "given")?.Value.Trim();
        var family = nameEl?.Element(Hl7Ns + "family")?.Value.Trim();

        if (!string.IsNullOrEmpty(given) || !string.IsNullOrEmpty(family))
        {
            var humanName = new HumanName { Use = HumanName.NameUse.Official };
            if (!string.IsNullOrEmpty(family)) humanName.Family = family;
            if (!string.IsNullOrEmpty(given)) humanName.Given = [given];
            practitioner.Name.Add(humanName);
        }

        return practitioner;
    }

    private Organization BuildOrganization(System.Xml.Linq.XDocument doc)
    {
        var org = new Organization { Id = Guid.NewGuid().ToString() };

        var custodianOrg = doc.Root
            ?.Element(Hl7Ns + "custodian")
            ?.Element(Hl7Ns + "assignedCustodian")
            ?.Element(Hl7Ns + "representedCustodianOrganization");

        var orgId = custodianOrg?.Element(Hl7Ns + "id")?.Attribute("root")?.Value;
        if (!string.IsNullOrEmpty(orgId))
        {
            org.Identifier.Add(new Identifier { System = $"urn:oid:{orgId}", Value = orgId });
        }

        var orgName = custodianOrg?.Element(Hl7Ns + "name")?.Value.Trim();
        if (!string.IsNullOrEmpty(orgName)) org.Name = orgName;

        return org;
    }

    private Composition BuildComposition(
        System.Xml.Linq.XDocument doc,
        Patient patient,
        Practitioner practitioner,
        Organization organization)
    {
        var composition = new Composition
        {
            Id = Guid.NewGuid().ToString(),
            Status = CompositionStatus.Final,
            Meta = new Meta { Profile = [EhdsCompositionProfile] }
        };

        // Type from ClinicalDocument/code
        var codeEl = doc.Root?.Element(Hl7Ns + "code");
        var code = codeEl?.Attribute("code")?.Value;
        var display = codeEl?.Attribute("displayName")?.Value;
        composition.Type = new CodeableConcept(LoincSystem, code ?? string.Empty, display ?? string.Empty, string.Empty);

        // Title
        var title = doc.Root?.Element(Hl7Ns + "title")?.Value.Trim();
        composition.Title = string.IsNullOrEmpty(title) ? display ?? string.Empty : title;

        // Date
        var effectiveValue = doc.Root?.Element(Hl7Ns + "effectiveTime")?.Attribute("value")?.Value;
        var effectiveDate = ParseCdaDate(effectiveValue);
        if (effectiveDate.HasValue)
            composition.Date = effectiveDate.Value.ToString("yyyy-MM-dd");

        // Language
        var langCode = doc.Root?.Element(Hl7Ns + "languageCode")?.Attribute("code")?.Value;
        if (!string.IsNullOrEmpty(langCode)) composition.Language = langCode;

        // Subject (patient)
        composition.Subject = new ResourceReference($"Patient/{patient.Id}");

        // Author (practitioner)
        composition.Author.Add(new ResourceReference($"Practitioner/{practitioner.Id}"));

        // Custodian (organization)
        composition.Custodian = new ResourceReference($"Organization/{organization.Id}");

        // Sections
        var sectionElements = doc.Root
            ?.Element(Hl7Ns + "component")
            ?.Element(Hl7Ns + "structuredBody")
            ?.Elements(Hl7Ns + "component")
            .Select(c => c.Element(Hl7Ns + "section"))
            .Where(s => s is not null)
            ?? Enumerable.Empty<XElement?>();

        foreach (var sectionEl in sectionElements)
        {
            if (sectionEl is null) continue;
            var section = new Composition.SectionComponent();
            var sectionCode = sectionEl.Element(Hl7Ns + "code");
            if (sectionCode is not null)
            {
                var sc = sectionCode.Attribute("code")?.Value;
                var sd = sectionCode.Attribute("displayName")?.Value;
                section.Code = new CodeableConcept(LoincSystem, sc ?? string.Empty, sd ?? string.Empty, string.Empty);
            }
            section.Title = sectionEl.Element(Hl7Ns + "title")?.Value.Trim() ?? string.Empty;
            composition.Section.Add(section);
        }

        return composition;
    }

    private static AdministrativeGender MapGender(string? cdaCode) =>
        cdaCode?.ToUpperInvariant() switch
        {
            "M" => AdministrativeGender.Male,
            "F" => AdministrativeGender.Female,
            "UN" => AdministrativeGender.Other,
            _ => AdministrativeGender.Unknown
        };

    private static DateOnly? ParseCdaDate(string? cdaDate)
    {
        if (string.IsNullOrEmpty(cdaDate) || cdaDate.Length < 8) return null;
        var datePart = cdaDate[..8];
        if (int.TryParse(datePart[..4], out var year)
            && int.TryParse(datePart[4..6], out var month)
            && int.TryParse(datePart[6..8], out var day))
        {
            return new DateOnly(year, month, day);
        }
        return null;
    }
}
