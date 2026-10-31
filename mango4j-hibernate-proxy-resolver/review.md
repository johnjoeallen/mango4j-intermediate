# Code and security review

Static review of the module's single production class, `HibernateProxyResolver`, and its POM. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation and line coverage 100%.

## Findings

1. **`hibernate-core` is a compile-scope dependency** (Low). `pom.xml:24` declares `hibernate-core` 7.2.1.Final with default scope, so every consumer inherits that Hibernate version transitively for a 31-line adapter. Consumers of this module already have Hibernate; declare it `provided`.

## Notes

- `HibernateProxyResolver.resolve` (`:16`) calls `getImplementation()`, which initialises the proxy. That can issue a query or throw `LazyInitializationException` when no session is open, so callers must account for the side effect.
