package ie.hl7.cda.fhir.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FHIR context and parser configuration beans.
 */
@Configuration
public class FhirConfig {

    /**
     * Creates and returns the FHIR R4 context (thread-safe singleton).
     *
     * @return configured FhirContext for R4
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    /**
     * Creates a JSON FHIR parser with pretty-printing enabled.
     *
     * @param fhirContext the shared FHIR context
     * @return JSON IParser instance
     */
    @Bean
    public IParser jsonParser(final FhirContext fhirContext) {
        return fhirContext.newJsonParser().setPrettyPrint(true);
    }

    /**
     * Creates an XML FHIR parser with pretty-printing enabled.
     *
     * @param fhirContext the shared FHIR context
     * @return XML IParser instance
     */
    @Bean
    public IParser xmlParser(final FhirContext fhirContext) {
        return fhirContext.newXmlParser().setPrettyPrint(true);
    }
}
