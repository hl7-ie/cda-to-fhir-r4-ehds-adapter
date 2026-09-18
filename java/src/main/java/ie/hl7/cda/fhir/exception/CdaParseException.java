package ie.hl7.cda.fhir.exception;

/**
 * Exception thrown when a CDA document cannot be parsed.
 */
public class CdaParseException extends RuntimeException {

    /**
     * Constructs a CdaParseException with the given message.
     *
     * @param message error description
     */
    public CdaParseException(final String message) {
        super(message);
    }

    /**
     * Constructs a CdaParseException with a message and a cause.
     *
     * @param message error description
     * @param cause   the underlying exception
     */
    public CdaParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
