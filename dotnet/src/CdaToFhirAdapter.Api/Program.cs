using CdaToFhirAdapter.Core.Mappers;
using CdaToFhirAdapter.Core.Parsers;
using CdaToFhirAdapter.Core.Services;

var builder = WebApplication.CreateBuilder(args);

// Register core services
builder.Services.AddSingleton<CdaParser>();
builder.Services.AddSingleton<CdaToFhirMapper>();
builder.Services.AddScoped<ConversionService>();

// MVC controllers + OpenAPI
builder.Services.AddControllers();
builder.Services.AddOpenApi();

var app = builder.Build();

// Configure pipeline
app.MapOpenApi();

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();

app.Run();

// Expose for integration testing
public partial class Program { }
