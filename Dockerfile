# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy dependency manifest first so Docker caches the dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR (skip tests — run them in CI separately)
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Non-root user for security
RUN addgroup --system agroconnect && adduser --system --ingroup agroconnect agroconnect

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/agroconnect-backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown agroconnect:agroconnect app.jar
USER agroconnect

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
