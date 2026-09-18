package ie.hl7.cda.fhir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CDA to FHIR R4 EHDS Adapter Spring Boot application.
 */
@SpringBootApplication
public class CdaToFhirApplication {

    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CdaToFhirApplication.class, args);
    }
}
