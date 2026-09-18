namespace CdaToFhirAdapter.Core.Exceptions;

/// <summary>Exception raised when CDA-to-FHIR mapping fails.</summary>
public sealed class FhirMappingException : Exception
{
    /// <summary>Initializes a new instance with a message.</summary>
    public FhirMappingException(string message) : base(message) { }

    /// <summary>Initializes a new instance with a message and inner exception.</summary>
    public FhirMappingException(string message, Exception inner) : base(message, inner) { }
}
