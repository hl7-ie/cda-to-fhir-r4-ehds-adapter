using CdaToFhirAdapter.Core.Mappers;
using CdaToFhirAdapter.Core.Parsers;
using CdaToFhirAdapter.Core.Services;
using Microsoft.Extensions.Logging;

// ---- Argument parsing ----
static void PrintHelp()
{
    Console.WriteLine("CDA to FHIR R4 EHDS Adapter – command-line interface");
    Console.WriteLine();
    Console.WriteLine("Usage:");
    Console.WriteLine("  cda-to-fhir --input <file> [--output <file>] [--format json|xml]");
    Console.WriteLine();
    Console.WriteLine("Options:");
    Console.WriteLine("  -i, --input   <file>   Path to the input CDA XML file (required)");
    Console.WriteLine("  -o, --output  <file>   Path for the output FHIR file (default: stdout)");
    Console.WriteLine("  -f, --format  <fmt>    Output format: json (default) or xml");
    Console.WriteLine("  -h, --help             Show this help message");
}

string? inputPath = null;
string? outputPath = null;
string format = "json";

for (int i = 0; i < args.Length; i++)
{
    switch (args[i])
    {
        case "-i":
        case "--input":
            if (i + 1 < args.Length) inputPath = args[++i];
            break;
        case "-o":
        case "--output":
            if (i + 1 < args.Length) outputPath = args[++i];
            break;
        case "-f":
        case "--format":
            if (i + 1 < args.Length) format = args[++i];
            break;
        case "-h":
        case "--help":
            PrintHelp();
            return 0;
    }
}

if (string.IsNullOrEmpty(inputPath))
{
    Console.Error.WriteLine("Error: --input is required.");
    PrintHelp();
    return 1;
}

if (!File.Exists(inputPath))
{
    Console.Error.WriteLine($"Error: input file '{inputPath}' not found.");
    return 1;
}

using var loggerFactory = LoggerFactory.Create(b => b.AddConsole().SetMinimumLevel(LogLevel.Warning));
var logger = loggerFactory.CreateLogger<ConversionService>();
var service = new ConversionService(new CdaToFhirMapper(new CdaParser()), logger);

try
{
    string cdaXml = await File.ReadAllTextAsync(inputPath);
    var result = service.Convert(cdaXml, format);

    if (!string.IsNullOrEmpty(outputPath))
    {
        var outDir = Path.GetDirectoryName(outputPath);
        if (!string.IsNullOrEmpty(outDir)) Directory.CreateDirectory(outDir);
        await File.WriteAllTextAsync(outputPath, result.FhirBundle);
        Console.WriteLine($"FHIR Bundle written to {outputPath}");
    }
    else
    {
        Console.WriteLine(result.FhirBundle);
    }

    foreach (var msg in result.ValidationMessages)
        Console.Error.WriteLine($"[WARN] {msg}");

    return 0;
}
catch (Exception ex)
{
    Console.Error.WriteLine($"Conversion failed: {ex.Message}");
    return 1;
}
