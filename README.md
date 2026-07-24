# CDA to FHIR R4 EHDS Adapter

[![Java CI](https://github.com/hl7-ie/cda-to-fhir-r4-ehds-adapter/actions/workflows/java-ci.yml/badge.svg)](https://github.com/hl7-ie/cda-to-fhir-r4-ehds-adapter/actions/workflows/java-ci.yml)
[![.NET CI](https://github.com/hl7-ie/cda-to-fhir-r4-ehds-adapter/actions/workflows/dotnet-ci.yml/badge.svg)](https://github.com/hl7-ie/cda-to-fhir-r4-ehds-adapter/actions/workflows/dotnet-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A reusable, enterprise-ready adapter for converting **HL7 CDA R2** clinical documents (aligned with the [European Health Data Space (EHDS)](https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space_en) specifications) into **FHIR R4** resources and document bundles.

---

## Goals

| Goal | How |
|------|-----|
| Standards-based CDA → FHIR R4 bridge | Declarative mappings per EHDS profile |
| EHDS-aligned profiles | Composition tagged with `http://hl7.eu/fhir/ehds/StructureDefinition/composition-eu-ehds` |
| REST API | Spring Boot (Java) / ASP.NET Core (.NET) |
| CLI | Picocli (Java) / built-in arg parser (.NET) |
| Code quality | Checkstyle, SpotBugs, OWASP Dependency Check (Java) / NuGet audit (.NET) |
| FHIR validation | HAPI FHIR validator (Java) / Firely SDK (.NET) |
| CI/CD | GitHub Actions with automated build, test, and security checks |

---

## Repository Layout

```
.
├── java/                          # Java / Spring Boot 3.2 implementation
│   ├── pom.xml
│   ├── checkstyle.xml
│   ├── spotbugs-exclude.xml
│   ├── owasp-suppressions.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/ie/hl7/cda/fhir/
│       │   ├── CdaToFhirApplication.java
│       │   ├── api/ConversionController.java
│       │   ├── cli/ConversionCommand.java
│       │   ├── config/FhirConfig.java
│       │   ├── exception/
│       │   ├── mapper/CdaToFhirMapper.java
│       │   ├── model/ConversionResult.java
│       │   ├── parser/CdaParser.java
│       │   └── service/ConversionService.java
│       └── test/
└── dotnet/                        # .NET 10 implementation
    ├── CdaToFhirAdapter.slnx
    ├── src/
    │   ├── CdaToFhirAdapter.Core/   # Shared parsing, mapping, service
    │   ├── CdaToFhirAdapter.Api/    # ASP.NET Core Web API
    │   └── CdaToFhirAdapter.Cli/    # Console CLI
    └── tests/
        └── CdaToFhirAdapter.Tests/  # xUnit tests
```

---

## Technology Stack

### Java

| Concern | Library / Tool |
|---------|---------------|
| Runtime | Java 17 (Temurin) |
| Framework | Spring Boot 3.2 |
| FHIR Model & Validation | HAPI FHIR R4 7.4 |
| OpenAPI | springdoc-openapi 2.5 |
| CLI | Picocli 4.7 |
| Build | Apache Maven 3.9 |
| Code style | Checkstyle 9 |
| Bug finder | SpotBugs 4.8 |
| Security | OWASP Dependency Check 10 |

### .NET

| Concern | Library / Tool |
|---------|---------------|
| Runtime | .NET 10 |
| Framework | ASP.NET Core 10 (Minimal / MVC) |
| FHIR Model & Validation | Firely .NET SDK (Hl7.Fhir.R4) 5.11 |
| CLI | Built-in arg parsing |
| Testing | xUnit 2.9, FluentAssertions 8.4 |
| Integration testing | Microsoft.AspNetCore.Mvc.Testing |
| Security | `dotnet list package --vulnerable` |

---

## Mapping Overview

| CDA Element | FHIR R4 Resource / Field |
|-------------|--------------------------|
| `ClinicalDocument/id` | `Bundle.identifier`, `Composition.identifier` |
| `ClinicalDocument/code` | `Composition.type` (LOINC) |
| `ClinicalDocument/title` | `Composition.title` |
| `ClinicalDocument/effectiveTime` | `Composition.date` |
| `ClinicalDocument/languageCode` | `Composition.language` |
| `recordTarget/patientRole/patient` | `Patient` (name, gender, DOB, address) |
| `author/assignedAuthor` | `Practitioner` |
| `custodian/assignedCustodian/representedCustodianOrganization` | `Organization` |
| `component/structuredBody/component/section` | `Composition.section[]` |

---

## Quick Start

### Java – REST API

```bash
cd java
mvn spring-boot:run
# API available at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

```bash
curl -X POST http://localhost:8080/api/v1/convert \
     -H "Content-Type: application/xml" \
     -H "Accept: application/json" \
     --data-binary @path/to/cda-document.xml
```

### Java – CLI

```bash
cd java
mvn package -DskipTests
java -jar target/cda-to-fhir-adapter-*.jar \
     --cli --input input.xml --output bundle.json --format json
```

### .NET – REST API

```bash
cd dotnet/src/CdaToFhirAdapter.Api
dotnet run
# API available at http://localhost:5000
# OpenAPI: http://localhost:5000/openapi/v1.json
```

### .NET – CLI

```bash
cd dotnet/src/CdaToFhirAdapter.Cli
dotnet run -- --input path/to/cda.xml --output bundle.json --format json
```

### Docker

```bash
# Java
docker build -t cda-fhir-java java/
docker run -p 8080:8080 cda-fhir-java

# .NET API
docker build -t cda-fhir-api -f dotnet/src/CdaToFhirAdapter.Api/Dockerfile dotnet/
docker run -p 8080:8080 cda-fhir-api
```

---

## Building & Testing

### Java

```bash
cd java
mvn checkstyle:check          # Style check
mvn test                      # Unit tests
mvn spotbugs:check            # Static analysis
mvn dependency-check:check    # OWASP CVE scan (requires NVD_API_KEY)
mvn package                   # Build fat JAR
```

### .NET

```bash
cd dotnet
dotnet build CdaToFhirAdapter.slnx
dotnet test CdaToFhirAdapter.slnx --collect:"XPlat Code Coverage"
dotnet list package --vulnerable --include-transitive
```

---

## CI/CD

| Workflow | Trigger | Steps |
|----------|---------|-------|
| `java-ci.yml` | Push / PR to `java/**` | Checkstyle → Build → Test → SpotBugs → OWASP |
| `dotnet-ci.yml` | Push / PR to `dotnet/**` | Restore → Build → Test → NuGet audit |

---

## Security

- CDA XML is parsed with XXE protection enabled (external entity and DTD processing disabled).
- Docker images run as non-root users.
- OWASP Dependency Check scans Java dependencies for known CVEs.
- NuGet vulnerability audit covers .NET transitive dependencies.

---

## License

MIT – see [LICENSE](LICENSE).
