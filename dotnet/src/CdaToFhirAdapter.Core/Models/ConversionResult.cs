namespace CdaToFhirAdapter.Core.Models;

/// <summary>
/// Holds the output of a CDA-to-FHIR conversion: the serialised FHIR payload
/// and any validation messages produced during the process.
/// </summary>
public sealed record ConversionResult(
    string FhirBundle,
    string ContentType,
    IReadOnlyList<string> ValidationMessages,
    bool IsValid);
