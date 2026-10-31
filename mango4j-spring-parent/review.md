# Code and security review

Reviewed the parent POM, module wiring, dependency and plugin configuration, and build-support file layout. The parent has no production Java code. This is a static review; no build was run and dependency or plugin vulnerabilities were not independently assessed.

## Finding

- **Checkstyle requires a specific sibling-directory layout** — `pom.xml:174` resolves its configuration through `${project.basedir}/../mango4j-spring-parent/checkstyle.xml`. A child built from a different checkout layout or using the published parent without the sibling directory can fail during the Checkstyle lifecycle. Package the rules as a reusable build artifact or explicitly constrain supported builds, and test a standalone child checkout.

The inherited Surefire `argLine` explicitly supplies the Mockito agent; child projects adding other agents need to compose their JVM arguments rather than replacing this value.


## Re-assessment (2026-10-31)

- **Altered:** PIT is 1.30.1 (unchanged here; the other trees moved up to it) and the `hammer-time` 100% defaults are overridden by lower per-module values in aop, logging, insights, observe, data and the CaaS delegate.
- The Checkstyle sibling-directory finding was carried forward without re-verification.
