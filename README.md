# AgroConnect Backend - Spring Boot

This is the backend for the B2B demand-driven fresh vegetable supply chain platform.

## Tech Stack
- Java 17+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Spring Security (JWT or session-based)

## Modules
- User Management
- Harvest
- Demand
- Order
- Pricing
- Freshness Tracking

## Getting Started
1. Configure your PostgreSQL database in `src/main/resources/application.properties`.
2. Build and run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Directory Structure
- `src/main/java/com/agroconnect/` - Java source code
- `src/main/resources/` - Configuration files

## Requirements
See requirements.md and system_design.md for details.
