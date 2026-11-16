# mango4j

Small, focused Java libraries: plain-Java utilities, and Spring Boot support modules built on them.
Everything in this repository is versioned and released together (currently `4.0.0-SNAPSHOT`),
and you depend only on the artifacts you need.

Group ID: `ie.bitstep.mango`

## Modules

### Plain Java (no Spring)

| Artifact | What it is for |
|---|---|
| `mango4j-collections` | Map and list builders, `MapUtils`, `CollectionReconciler` (diff a current collection against a desired one by key) |
| `mango4j-masking` | `Masker` implementations (PAN, account id, comma-separated lists, full/none) and the `@Mask` annotation |
| `mango4j-reflection` | Property accessors/getters/setters, `ReflectionUtils`, class and method info |
| `mango4j-utils` | Date and calendar helpers, `EntityToStringBuilder`, `MappingUtils`, `NamedScheduledExecutorBuilder`, conformance helpers |
| `mango4j-validation` | `ValidationUtils` and Jakarta Validation constraints such as `@Type4UUID`, `@IsValidKebabCase`, `@IsValidDottedCase` |
| `mango4j-hibernate-proxy-resolver` | `HibernateProxyResolver` for unwrapping Hibernate proxies |

### Spring Boot

| Artifact | What it is for |
|---|---|
| `mango4j-spring-aop` | Spring AOP support classes shared by the other Spring modules |
| `mango4j-spring-logging` | Annotation-based logging (uses `mango4j-masking`) |
| `mango4j-spring-insights` | Annotation-based metrics |
| `mango4j-spring-data` | Spring Data helpers such as `OffsetLimitPageable` |

Spring is kept out of the plain-Java modules by a Maven Enforcer rule (`no-spring` in
`mango4j-parent`), so the build fails if one of them gains a Spring dependency, directly or
transitively.

## Using the libraries

```xml
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-collections</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

See the [documentation](docs/index.md) for each module. Compatibility is listed in
[version compatibility](docs/mango4j-spring/version-compatibility.md).

## Contributions

All contributions are welcome. Please read [contributing.md](contributing.md) first.

## Development

### Prerequisites

- JDK 25
- Maven 3.9 or later
- Git

### Build

```bash
mvn clean install                # whole repository, run from this directory
mvn -pl mango4j-collections -am clean verify   # one module and what it needs
```

Checkstyle runs as part of the build. To run mutation testing with the project's thresholds:

```bash
mvn clean verify -Phammer-time
```

Publishing to Maven Central uses the `release` profile (GPG signing): `mvn deploy -Prelease`.

## Repository layout

```
mango4j/
  pom.xml                  reactor: builds everything
  mango4j-parent/          shared build config, checkstyle.xml, enforcer rule
  mango4j-spring-parent/   Spring-specific config (Boot BOM), inherits from mango4j-parent
  mango4j-<module>/        plain-Java modules
  mango4j-spring-<module>/ Spring modules
  docs/                    documentation
```

## Licence

Apache License 2.0, see [LICENSE](LICENSE).
