# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY checkstyle.xml .
COPY spotbugs-exclude.xml .
COPY owasp-suppressions.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -q -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
