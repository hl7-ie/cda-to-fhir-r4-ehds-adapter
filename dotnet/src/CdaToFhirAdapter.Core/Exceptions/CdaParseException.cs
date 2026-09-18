namespace CdaToFhirAdapter.Core.Exceptions;

/// <summary>Exception raised when CDA XML cannot be parsed.</summary>
public sealed class CdaParseException : Exception
{
    /// <summary>Initializes a new instance with a message.</summary>
    public CdaParseException(string message) : base(message) { }

    /// <summary>Initializes a new instance with a message and inner exception.</summary>
    public CdaParseException(string message, Exception inner) : base(message, inner) { }
}
