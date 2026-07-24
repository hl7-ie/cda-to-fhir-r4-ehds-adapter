using CdaToFhirAdapter.Core.Exceptions;
using CdaToFhirAdapter.Core.Services;
using Microsoft.AspNetCore.Mvc;

namespace CdaToFhirAdapter.Api.Controllers;

/// <summary>REST controller exposing the CDA-to-FHIR R4 conversion endpoint.</summary>
[ApiController]
[Route("api/v1")]
public sealed class ConversionController : ControllerBase
{
    private readonly ConversionService _conversionService;
    private readonly ILogger<ConversionController> _logger;

    /// <summary>Constructs the controller.</summary>
    public ConversionController(ConversionService conversionService,
        ILogger<ConversionController> logger)
    {
        _conversionService = conversionService;
        _logger = logger;
    }

    /// <summary>
    /// Converts an HL7 CDA R2 document to a FHIR R4 document Bundle.
    /// </summary>
    /// <param name="format">Output format: <c>json</c> (default) or <c>xml</c>.</param>
    /// <returns>Serialised FHIR Bundle.</returns>
    /// <response code="200">Conversion successful.</response>
    /// <response code="400">Invalid or malformed CDA document.</response>
    /// <response code="422">CDA parsed but FHIR mapping failed.</response>
    [HttpPost("convert")]
    [Consumes("application/xml", "text/xml")]
    [Produces("application/json", "application/xml")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status422UnprocessableEntity)]
    public IActionResult Convert(
        [FromBody] string cdaXml,
        [FromQuery] string format = "json")
    {
        // Sanitise user-provided format to prevent log injection
        var safeFormat = format is "json" or "xml" ? format : "json";
        _logger.LogInformation("Received CDA conversion request, format={Format}", safeFormat);

        try
        {
            var result = _conversionService.Convert(cdaXml, safeFormat);
            return Content(result.FhirBundle, result.ContentType);
        }
        catch (CdaParseException ex)
        {
            _logger.LogWarning(ex, "CDA parse error");
            return Problem(
                detail: ex.Message,
                title: "Invalid CDA document",
                statusCode: StatusCodes.Status400BadRequest);
        }
        catch (FhirMappingException ex)
        {
            _logger.LogWarning(ex, "FHIR mapping error");
            return Problem(
                detail: ex.Message,
                title: "FHIR mapping failed",
                statusCode: StatusCodes.Status422UnprocessableEntity);
        }
    }

    /// <summary>Returns a liveness probe response.</summary>
    [HttpGet("health")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public IActionResult Health() => Ok(new { status = "healthy" });
}
