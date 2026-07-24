package ie.hl7.cda.fhir.mapper;

import ie.hl7.cda.fhir.exception.FhirMappingException;
import ie.hl7.cda.fhir.parser.CdaParser;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;

/**
 * Maps a parsed CDA R2 DOM document to a FHIR R4 {@link Bundle} of type
 * {@code document}.
 *
 * <p>The following resources are created:</p>
 * <ul>
 *   <li>{@link Composition} – from the CDA document header</li>
 *   <li>{@link Patient} – from {@code recordTarget/patientRole}</li>
 *   <li>{@link Practitioner} – from the first {@code author} element</li>
 *   <li>{@link Organization} – from the {@code custodian}</li>
 * </ul>
 */
@Component
public class CdaToFhirMapper {

    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String HL7_GENDER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender";
    private static final String PROFILE_COMPOSITION =
            "http://hl7.eu/fhir/ehds/StructureDefinition/composition-eu-ehds";

    private final CdaParser cdaParser;

    /**
     * Constructs the mapper with the required {@link CdaParser}.
     *
     * @param cdaParser the CDA XML parser
     */
    public CdaToFhirMapper(final CdaParser cdaParser) {
        this.cdaParser = cdaParser;
    }

    /**
     * Converts a CDA XML document string into a FHIR R4 Bundle.
     *
     * @param cdaXml the raw CDA XML string
     * @return a FHIR {@link Bundle} of type {@code document}
     * @throws FhirMappingException if the mapping fails
     */
    public Bundle map(final String cdaXml) {
        Document doc;
        try {
            doc = cdaParser.parse(cdaXml);
        } catch (Exception ex) {
            throw new FhirMappingException("CDA parsing failed: " + ex.getMessage(), ex);
        }

        // Create resources
        Patient patient = buildPatient(doc);
        Practitioner practitioner = buildPractitioner(doc);
        Organization organization = buildOrganization(doc);
        Composition composition = buildComposition(doc, patient, practitioner, organization);

        // Assemble bundle
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.setTimestamp(new Date());

        String docId = cdaParser.extractAttribute(doc, "/cda:ClinicalDocument/cda:id", "root");
        String docExt = cdaParser.extractAttribute(doc, "/cda:ClinicalDocument/cda:id", "extension");
        if (!docId.isEmpty()) {
            Identifier id = new Identifier().setSystem("urn:oid:" + docId);
            if (!docExt.isEmpty()) {
                id.setValue(docExt);
            }
            bundle.setIdentifier(id);
        }

        bundle.addEntry().setFullUrl("urn:uuid:" + composition.getId()).setResource(composition);
        bundle.addEntry().setFullUrl("urn:uuid:" + patient.getId()).setResource(patient);
        bundle.addEntry().setFullUrl("urn:uuid:" + practitioner.getId()).setResource(practitioner);
        bundle.addEntry().setFullUrl("urn:uuid:" + organization.getId()).setResource(organization);

        return bundle;
    }

    private Patient buildPatient(final Document doc) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());

        // Identifier from patientRole/id
        String idRoot = cdaParser.extractAttribute(
                doc, "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:id", "root");
        String idExt = cdaParser.extractAttribute(
                doc, "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:id", "extension");
        if (!idRoot.isEmpty()) {
            patient.addIdentifier(new Identifier()
                    .setSystem("urn:oid:" + idRoot)
                    .setValue(idExt.isEmpty() ? idRoot : idExt));
        }

        // Name
        String given = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:patient/cda:name/cda:given");
        String family = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:patient/cda:name/cda:family");
        if (!given.isEmpty() || !family.isEmpty()) {
            HumanName name = new HumanName().setUse(HumanName.NameUse.OFFICIAL);
            if (!family.isEmpty()) {
                name.setFamily(family);
            }
            if (!given.isEmpty()) {
                name.addGiven(given);
            }
            patient.addName(name);
        }

        // Gender
        String genderCode = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:patient/cda:administrativeGenderCode",
                "code");
        patient.setGender(mapGender(genderCode));

        // Birth date
        String birthTime = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:patient/cda:birthTime",
                "value");
        parseCdaDate(birthTime).ifPresent(patient::setBirthDate);

        // Address
        String street = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:addr/cda:streetAddressLine");
        String city = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:addr/cda:city");
        String country = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:addr/cda:country");
        if (!street.isEmpty() || !city.isEmpty()) {
            Address address = new Address();
            if (!street.isEmpty()) {
                address.addLine(street);
            }
            if (!city.isEmpty()) {
                address.setCity(city);
            }
            if (!country.isEmpty()) {
                address.setCountry(country);
            }
            patient.addAddress(address);
        }

        // Telecom
        String telecom = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:recordTarget/cda:patientRole/cda:telecom", "value");
        if (!telecom.isEmpty()) {
            patient.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(telecom.replaceFirst("^tel:", "")));
        }

        return patient;
    }

    private Practitioner buildPractitioner(final Document doc) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());

        String idRoot = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:author/cda:assignedAuthor/cda:id", "root");
        String idExt = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:author/cda:assignedAuthor/cda:id", "extension");
        if (!idRoot.isEmpty()) {
            practitioner.addIdentifier(new Identifier()
                    .setSystem("urn:oid:" + idRoot)
                    .setValue(idExt.isEmpty() ? idRoot : idExt));
        }

        String given = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:author/cda:assignedAuthor/cda:assignedPerson/cda:name/cda:given");
        String family = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:author/cda:assignedAuthor/cda:assignedPerson/cda:name/cda:family");
        if (!given.isEmpty() || !family.isEmpty()) {
            HumanName name = new HumanName().setUse(HumanName.NameUse.OFFICIAL);
            if (!family.isEmpty()) {
                name.setFamily(family);
            }
            if (!given.isEmpty()) {
                name.addGiven(given);
            }
            practitioner.addName(name);
        }

        return practitioner;
    }

    private Organization buildOrganization(final Document doc) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID().toString());

        String orgId = cdaParser.extractAttribute(doc,
                "/cda:ClinicalDocument/cda:custodian/cda:assignedCustodian"
                        + "/cda:representedCustodianOrganization/cda:id", "root");
        if (!orgId.isEmpty()) {
            org.addIdentifier(new Identifier().setSystem("urn:oid:" + orgId).setValue(orgId));
        }

        String orgName = cdaParser.extractText(doc,
                "/cda:ClinicalDocument/cda:custodian/cda:assignedCustodian"
                        + "/cda:representedCustodianOrganization/cda:name");
        if (!orgName.isEmpty()) {
            org.setName(orgName);
        }

        return org;
    }

    private Composition buildComposition(final Document doc, final Patient patient,
            final Practitioner practitioner, final Organization organization) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setMeta(new Meta().addProfile(PROFILE_COMPOSITION));
        composition.setStatus(Composition.CompositionStatus.FINAL);

        // type from ClinicalDocument/code
        String code = cdaParser.extractAttribute(doc, "/cda:ClinicalDocument/cda:code", "code");
        String display = cdaParser.extractAttribute(doc, "/cda:ClinicalDocument/cda:code", "displayName");
        composition.setType(new CodeableConcept()
                .addCoding(new Coding().setSystem(LOINC_SYSTEM).setCode(code).setDisplay(display)));

        // title
        String title = cdaParser.extractText(doc, "/cda:ClinicalDocument/cda:title");
        composition.setTitle(title.isEmpty() ? display : title);

        // date
        String effectiveTime = cdaParser.extractAttribute(
                doc, "/cda:ClinicalDocument/cda:effectiveTime", "value");
        parseCdaDate(effectiveTime).ifPresent(d ->
                composition.setDate(d));

        // language
        String langCode = cdaParser.extractAttribute(
                doc, "/cda:ClinicalDocument/cda:languageCode", "code");
        if (!langCode.isEmpty()) {
            composition.setLanguage(langCode);
        }

        // subject (patient)
        composition.setSubject(new Reference("Patient/" + patient.getId()));

        // author (practitioner)
        composition.addAuthor(new Reference("Practitioner/" + practitioner.getId()));

        // custodian (organization)
        composition.setCustodian(new Reference("Organization/" + organization.getId()));

        // Sections
        NodeList sectionNodes = cdaParser.extractNodes(doc,
                "/cda:ClinicalDocument/cda:component/cda:structuredBody/cda:component/cda:section");
        for (int i = 0; i < sectionNodes.getLength(); i++) {
            Node sectionNode = sectionNodes.item(i);
            Composition.SectionComponent section = buildSection(sectionNode);
            composition.addSection(section);
        }

        return composition;
    }

    private Composition.SectionComponent buildSection(final Node sectionNode) {
        Composition.SectionComponent section = new Composition.SectionComponent();
        NodeList children = sectionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("code".equals(child.getLocalName())) {
                Node codeAttr = child.getAttributes().getNamedItem("code");
                Node displayAttr = child.getAttributes().getNamedItem("displayName");
                if (codeAttr != null) {
                    section.setCode(new CodeableConcept().addCoding(
                            new Coding().setSystem(LOINC_SYSTEM)
                                    .setCode(codeAttr.getNodeValue())
                                    .setDisplay(displayAttr != null ? displayAttr.getNodeValue() : "")));
                }
            } else if ("title".equals(child.getLocalName())) {
                section.setTitle(child.getTextContent().trim());
            }
        }
        return section;
    }

    private Enumerations.AdministrativeGender mapGender(final String cdaCode) {
        if ("M".equalsIgnoreCase(cdaCode)) {
            return Enumerations.AdministrativeGender.MALE;
        }
        if ("F".equalsIgnoreCase(cdaCode)) {
            return Enumerations.AdministrativeGender.FEMALE;
        }
        if ("UN".equalsIgnoreCase(cdaCode)) {
            return Enumerations.AdministrativeGender.OTHER;
        }
        return Enumerations.AdministrativeGender.UNKNOWN;
    }

    private java.util.Optional<Date> parseCdaDate(final String cdaDate) {
        if (cdaDate == null || cdaDate.isEmpty()) {
            return java.util.Optional.empty();
        }
        String trimmed = cdaDate.length() >= 8 ? cdaDate.substring(0, 8) : cdaDate;
        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return java.util.Optional.of(Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant()));
        } catch (DateTimeParseException ex) {
            return java.util.Optional.empty();
        }
    }
}
