# mango4j

Small, focused Java libraries: plain-Java utilities, and Spring Boot support modules built on them.
Everything in this repository is versioned and released together (currently `4.0.0-SNAPSHOT`),
and you depend only on the artifacts you need.

Group ID: `ie.bitstep.mango`

## Modules

No Spring dependencies. Spring support is in [mango4j-spring](https://github.com/bitstep-ie/mango4j-spring), which builds on this repository; the naming rule is `mango4j-*` for plain Java and `mango4j-spring-*` for Spring.

| Artifact | What it is for |
|---|---|
| `mango4j-collections` | Map and list builders, `MapUtils`, `CollectionReconciler` (diff a current collection against a desired one by key) |
| `mango4j-masking` | `Masker` implementations (PAN, account id, comma-separated lists, full/none) and the `@Mask` annotation |
| `mango4j-reflection` | Property accessors/getters/setters, `ReflectionUtils`, class and method info |
| `mango4j-utils` | Date and calendar helpers, `EntityToStringBuilder`, `MappingUtils`, `NamedScheduledExecutorBuilder`, conformance helpers |
| `mango4j-validation` | `ValidationUtils` and Jakarta Validation constraints such as `@Type4UUID`, `@IsValidKebabCase`, `@IsValidDottedCase` |
| `mango4j-hibernate-proxy-resolver` | `HibernateProxyResolver` for unwrapping Hibernate proxies |
| `mango4j-build-tools` | Shared Checkstyle configuration used by the build (not for applications) |

A Maven Enforcer rule (`no-spring` in `mango4j-parent`) fails the build if any module here gains a Spring dependency, directly or transitively.

## Using the libraries

```xml
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-collections</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

See the [documentation](docs/index.md) for each module.

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
  mango4j-parent/          shared build config and the enforcer rule
  mango4j-build-tools/     shared Checkstyle rules, consumed by mango4j-parent
  mango4j-<module>/        the modules
  docs/                    documentation
```

## Licence

Apache License 2.0, see [LICENSE](LICENSE).
