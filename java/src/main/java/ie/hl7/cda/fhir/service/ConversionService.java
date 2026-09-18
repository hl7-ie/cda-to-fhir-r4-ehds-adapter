package ie.hl7.cda.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import ie.hl7.cda.fhir.mapper.CdaToFhirMapper;
import ie.hl7.cda.fhir.model.ConversionResult;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the CDA-to-FHIR R4 conversion pipeline:
 * <ol>
 *   <li>Delegate mapping to {@link CdaToFhirMapper}</li>
 *   <li>Validate the resulting Bundle via HAPI FHIR validation</li>
 *   <li>Serialise the Bundle to JSON or XML</li>
 * </ol>
 */
@Service
public class ConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(ConversionService.class);

    private final CdaToFhirMapper mapper;
    private final FhirContext fhirContext;

    /**
     * Constructs the service.
     *
     * @param mapper      the CDA-to-FHIR mapper
     * @param fhirContext the shared FHIR R4 context
     */
    public ConversionService(final CdaToFhirMapper mapper, final FhirContext fhirContext) {
        this.mapper = mapper;
        this.fhirContext = fhirContext;
    }

    /**
     * Converts a CDA XML document to a FHIR R4 Bundle serialised as JSON.
     *
     * @param cdaXml the CDA document XML string
     * @return the conversion result containing the serialised Bundle
     */
    public ConversionResult convert(final String cdaXml) {
        return convert(cdaXml, "json");
    }

    /**
     * Converts a CDA XML document to a FHIR R4 Bundle.
     *
     * @param cdaXml the CDA document XML string
     * @param format either {@code "json"} or {@code "xml"}
     * @return the conversion result containing the serialised Bundle
     */
    public ConversionResult convert(final String cdaXml, final String format) {
        LOG.info("Starting CDA-to-FHIR conversion, output format={}", format);

        Bundle bundle = mapper.map(cdaXml);

        // Validate
        FhirValidator validator = fhirContext.newValidator();
        ValidationResult validationResult = validator.validateWithResult(bundle);
        List<String> messages = validationResult.getMessages().stream()
                .map(m -> m.getSeverity() + ": " + m.getMessage())
                .collect(Collectors.toList());

        boolean isXml = "xml".equalsIgnoreCase(format);
        IParser parser = isXml
                ? fhirContext.newXmlParser().setPrettyPrint(true)
                : fhirContext.newJsonParser().setPrettyPrint(true);
        String serialised = parser.encodeResourceToString(bundle);
        String contentType = isXml ? MediaType.APPLICATION_XML_VALUE : MediaType.APPLICATION_JSON_VALUE;

        LOG.info("CDA-to-FHIR conversion complete, valid={}, messages={}",
                validationResult.isSuccessful(), messages.size());

        return ConversionResult.builder()
                .fhirBundle(serialised)
                .contentType(contentType)
                .validationMessages(messages)
                .valid(validationResult.isSuccessful())
                .build();
    }
}
