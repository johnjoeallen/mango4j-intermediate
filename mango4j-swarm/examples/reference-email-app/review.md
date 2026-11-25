# Code and security review

Static review of the application classes, `application.yml`, migrations, POM and run script. The app is outside the Maven reactor and was not run against PostgreSQL. Dependencies were not scanned for known vulnerabilities.

## Findings

1. **The example targets a runtime that cannot load the library** (Medium). `pom.xml:10,13` set Java 17 and Spring Boot 3.5.15, while `mango-swarm` is Java 25 bytecode (class-file major version 69) built for Spring Boot 4.1. The application compiles against the 4.0.0-SNAPSHOT jar but a Java 17 JVM cannot load those classes. Move the example to Java 25 and the Boot 4 BOM.

2. **The sample handler logs customer and recipient details** (Low). `SendEmailTaskHandler.java:28` logs the customer id at INFO and `:32` logs recipient and subject at TRACE. The seed data is synthetic, but copying the handler into a real application would put personal data in logs.

## Notes

- Database credentials come from environment variables (`${POSTGRES_USER}`, `${POSTGRES_PASSWORD}`), not from the repository.
- The handler only simulates sending; production email delivery must handle retries and duplicate side effects as described in the Swarm review.
