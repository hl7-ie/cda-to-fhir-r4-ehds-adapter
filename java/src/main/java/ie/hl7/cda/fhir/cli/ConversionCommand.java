package ie.hl7.cda.fhir.cli;

import ie.hl7.cda.fhir.model.ConversionResult;
import ie.hl7.cda.fhir.service.ConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the CDA-to-FHIR adapter.
 *
 * <p>Usage examples:
 * <pre>
 *   # Convert to JSON (default):
 *   java -jar adapter.jar --cli --input input.xml --output output.json
 *
 *   # Convert to XML:
 *   java -jar adapter.jar --cli --input input.xml --output output.xml --format xml
 * </pre>
 * </p>
 */
@Component
@Command(name = "cda-to-fhir",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Converts HL7 CDA R2 documents to FHIR R4 Bundles")
public class ConversionCommand implements Callable<Integer>, CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ConversionCommand.class);

    @Option(names = {"--cli"}, description = "Run in CLI mode (skip REST server startup)")
    private boolean cliMode;

    @Option(names = {"-i", "--input"}, description = "Input CDA XML file path")
    private File inputFile;

    @Option(names = {"-o", "--output"}, description = "Output FHIR file path")
    private File outputFile;

    @Option(names = {"-f", "--format"}, description = "Output format: json (default) or xml",
            defaultValue = "json")
    private String format;

    private final ConversionService conversionService;

    /**
     * Constructs the CLI command.
     *
     * @param conversionService the conversion service
     */
    public ConversionCommand(final ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Spring {@link CommandLineRunner} entry point – invoked on application startup.
     *
     * @param args raw command-line arguments
     */
    @Override
    public void run(final String... args) {
        new CommandLine(this).execute(args);
    }

    /**
     * Picocli callable – executes the conversion when CLI mode is active.
     *
     * @return exit code (0 = success, 1 = error)
     */
    @Override
    public Integer call() {
        if (!cliMode || inputFile == null) {
            // Not in CLI mode – REST server mode is active
            return 0;
        }
        try {
            String cdaXml = Files.readString(inputFile.toPath());
            ConversionResult result = conversionService.convert(cdaXml, format);

            if (outputFile != null) {
                Path out = outputFile.toPath();
                if (out.getParent() != null) {
                    Files.createDirectories(out.getParent());
                }
                Files.writeString(out, result.getFhirBundle());
                LOG.info("FHIR Bundle written to {}", outputFile.getAbsolutePath());
            } else {
                System.out.println(result.getFhirBundle());
            }

            if (!result.isValid()) {
                LOG.warn("Validation warnings:");
                result.getValidationMessages().forEach(msg -> LOG.warn("  {}", msg));
            }
            return 0;
        } catch (IOException ex) {
            LOG.error("I/O error during conversion: {}", ex.getMessage(), ex);
            return 1;
        } catch (Exception ex) {
            LOG.error("Conversion failed: {}", ex.getMessage(), ex);
            return 1;
        }
    }
}
