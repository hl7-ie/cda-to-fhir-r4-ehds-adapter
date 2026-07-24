package ie.hl7.cda.fhir.exception;

/**
 * Exception thrown when the CDA-to-FHIR mapping fails.
 */
public class FhirMappingException extends RuntimeException {

    /**
     * Constructs a FhirMappingException with the given message.
     *
     * @param message error description
     */
    public FhirMappingException(final String message) {
        super(message);
    }

    /**
     * Constructs a FhirMappingException with a message and a cause.
     *
     * @param message error description
     * @param cause   the underlying exception
     */
    public FhirMappingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
