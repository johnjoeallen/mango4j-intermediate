# Code and security review

Reviewed the production application classes, configuration, migrations, POM, and running instructions. This is a static review; tests and a live PostgreSQL run were not performed, and dependency vulnerabilities were not independently assessed.

## Findings

- **Reference application targets an incompatible runtime and Spring generation** — `pom.xml:10` sets Java 17 and Spring Boot 3.5, while the current `mango-swarm` POM targets Java 25 and Spring Boot 4.1 and uses the Boot 4 Hibernate auto-configuration package. Building/running the example as a Java 17/Boot 3 application cannot reliably load the current library. Align the supported Java and Spring versions and add a build-and-start integration check against the reactor artifact.
- **Reference handler logs potentially sensitive email metadata** — `src/main/java/ie/bitstep/mango/spring/swarm/examples/email/SendEmailTaskHandler.java:30` logs customer IDs at INFO and recipient and subject at TRACE. The included seed data is synthetic, but copying this handler into a real application can disclose personal or sensitive data. Use redacted or synthetic-only logging and make the example's limitation explicit.

The handler does not send email; its configured idempotency and timeout behavior is suitable only for the simulated operation. Production email delivery must account for retries and duplicate side effects as documented in the parent Swarm review.


## Re-assessment (2026-10-31)

- **Altered:** the app's own version and `mango-swarm.version` were bumped from `0.1.0-SNAPSHOT` to `4.0.0-SNAPSHOT`, so it now resolves; the package imports were updated for the swarm rename. `mvn compile` succeeds.
- **Still open, and confirmed:** the app still targets Java 17 and Spring Boot 3.5.15 (`java.version=17`) while the `mango-swarm` jar is Java 25 bytecode (class major version 69) built for Boot 4.1.2. It compiles, but a Java 17 JVM cannot load those classes, so the app cannot run as configured. Raise `java.version` to 25 and move to the Boot 4 BOM.
- Scope: the app is outside the Maven reactor, so no build, test or PIT run covers it.
