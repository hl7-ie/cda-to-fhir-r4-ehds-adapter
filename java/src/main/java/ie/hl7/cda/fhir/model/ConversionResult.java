package ie.hl7.cda.fhir.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Holds the result of a CDA-to-FHIR conversion: the serialised FHIR payload
 * and any validation messages.
 */
@Getter
@Builder
public class ConversionResult {

    /** Serialised FHIR Bundle (JSON or XML). */
    private final String fhirBundle;

    /** Mime-type of {@link #fhirBundle}. */
    private final String contentType;

    /** Validation messages produced during conversion. */
    private final List<String> validationMessages;

    /** Whether the FHIR validation passed. */
    private final boolean valid;
}
