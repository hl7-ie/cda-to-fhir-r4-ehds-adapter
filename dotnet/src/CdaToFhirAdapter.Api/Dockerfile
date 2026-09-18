# Build stage
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY . .
RUN dotnet restore src/CdaToFhirAdapter.Api/CdaToFhirAdapter.Api.csproj
RUN dotnet publish src/CdaToFhirAdapter.Api/CdaToFhirAdapter.Api.csproj \
    --configuration Release \
    --output /app/publish \
    --no-restore

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
RUN adduser --system --no-create-home appuser && addgroup --system appgroup \
    && adduser appuser appgroup
WORKDIR /app
COPY --from=build /app/publish .
USER appuser
EXPOSE 8080
ENTRYPOINT ["dotnet", "CdaToFhirAdapter.Api.dll"]
