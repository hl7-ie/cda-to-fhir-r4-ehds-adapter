package ie.hl7.cda.fhir.mapper;

import ie.hl7.cda.fhir.parser.CdaParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link CdaToFhirMapper}.
 */
class CdaToFhirMapperTest {

    private CdaToFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CdaToFhirMapper(new CdaParser());
    }

    @Test
    @DisplayName("map() produces a DOCUMENT bundle from a valid CDA")
    void mapProducesDocumentBundle() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        assertNotNull(bundle);
        assertEquals(Bundle.BundleType.DOCUMENT, bundle.getType());
    }

    @Test
    @DisplayName("map() creates Composition with correct LOINC code")
    void mapCreatesCompositionWithLoincCode() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        Composition composition = (Composition) bundle.getEntry().get(0).getResource();
        assertNotNull(composition.getType());
        assertEquals("60591-5", composition.getType().getCodingFirstRep().getCode());
    }

    @Test
    @DisplayName("map() creates Patient with correct name")
    void mapCreatesPatientWithName() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        Patient patient = (Patient) bundle.getEntry().get(1).getResource();
        assertNotNull(patient.getName());
        assertFalse(patient.getName().isEmpty());
        assertEquals("Doe", patient.getNameFirstRep().getFamily());
        assertEquals("John", patient.getNameFirstRep().getGivenAsSingleString());
    }

    @Test
    @DisplayName("map() maps patient gender correctly")
    void mapCreatesPatientWithGender() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        Patient patient = (Patient) bundle.getEntry().get(1).getResource();
        assertEquals(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE, patient.getGender());
    }

    @Test
    @DisplayName("map() creates Practitioner with correct name")
    void mapCreatesPractitionerWithName() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        Practitioner practitioner = (Practitioner) bundle.getEntry().get(2).getResource();
        assertNotNull(practitioner.getName());
        assertFalse(practitioner.getName().isEmpty());
        assertEquals("Smith", practitioner.getNameFirstRep().getFamily());
    }

    @Test
    @DisplayName("map() creates sections matching CDA structured body")
    void mapCreatesCompositionSections() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        Composition composition = (Composition) bundle.getEntry().get(0).getResource();
        assertEquals(2, composition.getSection().size());
        assertEquals("Active Problems", composition.getSection().get(0).getTitle());
    }

    @Test
    @DisplayName("map() sets bundle identifier from CDA document id")
    void mapSetsBundleIdentifier() throws IOException {
        Bundle bundle = mapper.map(loadSampleCda());

        assertNotNull(bundle.getIdentifier());
        assertEquals("PS-2024-001", bundle.getIdentifier().getValue());
    }

    private String loadSampleCda() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/sample-patient-summary.xml")) {
            assertNotNull(is, "Sample CDA resource not found");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
