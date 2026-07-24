using CdaToFhirAdapter.Core.Mappers;
using CdaToFhirAdapter.Core.Models;
using CdaToFhirAdapter.Core.Parsers;
using Hl7.Fhir.Model;
using Hl7.Fhir.Serialization;
using Microsoft.Extensions.Logging;

namespace CdaToFhirAdapter.Core.Services;

/// <summary>
/// Orchestrates the CDA-to-FHIR R4 conversion pipeline:
/// map → validate → serialise.
/// </summary>
public sealed class ConversionService
{
    private readonly CdaToFhirMapper _mapper;
    private readonly ILogger<ConversionService> _logger;

    /// <summary>Constructs the service.</summary>
    public ConversionService(CdaToFhirMapper mapper, ILogger<ConversionService> logger)
    {
        _mapper = mapper;
        _logger = logger;
    }

    /// <summary>Converts a CDA XML string to a serialised FHIR R4 Bundle (JSON by default).</summary>
    public ConversionResult Convert(string cdaXml, string format = "json")
    {
        _logger.LogInformation("Starting CDA-to-FHIR conversion, format={Format}", format);

        Bundle bundle = _mapper.Map(cdaXml);

        bool isXml = format.Equals("xml", StringComparison.OrdinalIgnoreCase);

        string serialised;
        string contentType;

        if (isXml)
        {
            var serializer = new FhirXmlSerializer(new SerializerSettings { Pretty = true });
            serialised = serializer.SerializeToString(bundle);
            contentType = "application/xml";
        }
        else
        {
            var serializer = new FhirJsonSerializer(new SerializerSettings { Pretty = true });
            serialised = serializer.SerializeToString(bundle);
            contentType = "application/json";
        }

        // Basic validation – check required fields
        var messages = new List<string>();
        if (bundle.Entry.Count == 0)
            messages.Add("Warning: Bundle contains no entries.");

        bool isValid = messages.Count == 0;

        _logger.LogInformation(
            "CDA-to-FHIR conversion complete, entries={Count}, valid={Valid}",
            bundle.Entry.Count, isValid);

        return new ConversionResult(serialised, contentType, messages, isValid);
    }
}
