package ie.hl7.cda.fhir.api;

import ie.hl7.cda.fhir.exception.CdaParseException;
import ie.hl7.cda.fhir.exception.FhirMappingException;
import ie.hl7.cda.fhir.model.ConversionResult;
import ie.hl7.cda.fhir.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller that exposes the CDA-to-FHIR R4 conversion endpoint.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Conversion", description = "CDA to FHIR R4 conversion operations")
public class ConversionController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversionController.class);

    private final ConversionService conversionService;

    /**
     * Constructs the controller.
     *
     * @param conversionService the conversion service
     */
    public ConversionController(final ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Converts a CDA XML document to a FHIR R4 Bundle.
     *
     * @param cdaXml the CDA XML document body
     * @param format the desired output format ({@code json} or {@code xml}); defaults to {@code json}
     * @return {@code 200 OK} with the FHIR Bundle, or an error response
     */
    @PostMapping(value = "/convert",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Convert CDA document to FHIR R4 Bundle",
            description = "Accepts an HL7 CDA R2 document and returns a FHIR R4 document Bundle",
            responses = {
                @ApiResponse(responseCode = "200", description = "Conversion successful",
                        content = @Content(mediaType = "application/json",
                                schema = @Schema(type = "string", format = "FHIR Bundle JSON"))),
                @ApiResponse(responseCode = "400", description = "Invalid CDA input"),
                @ApiResponse(responseCode = "422", description = "CDA parsed but FHIR mapping failed")
            })
    public ResponseEntity<String> convert(
            @RequestBody final String cdaXml,
            @Parameter(description = "Output format: json (default) or xml")
            @RequestParam(defaultValue = "json") final String format) {

        LOG.info("Received CDA conversion request, format={}", format);
        ConversionResult result = conversionService.convert(cdaXml, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getFhirBundle());
    }

    /**
     * Global exception handler for {@link CdaParseException}.
     *
     * @param ex the exception
     * @return {@code 400 Bad Request} with the error message
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(CdaParseException.class)
    public ResponseEntity<Map<String, String>> handleCdaParseException(final CdaParseException ex) {
        LOG.warn("CDA parse error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid CDA document", "detail", ex.getMessage()));
    }

    /**
     * Global exception handler for {@link FhirMappingException}.
     *
     * @param ex the exception
     * @return {@code 422 Unprocessable Entity} with the error message
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(FhirMappingException.class)
    public ResponseEntity<Map<String, String>> handleMappingException(final FhirMappingException ex) {
        LOG.warn("FHIR mapping error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "FHIR mapping failed", "detail", ex.getMessage()));
    }
}
