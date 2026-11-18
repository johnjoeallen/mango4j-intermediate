# mango4j-spring

Spring Boot support libraries, built on the plain-Java [mango4j](https://github.com/bitstep-ie/mango4j)
modules. Everything here shares one version (currently `4.0.0-SNAPSHOT`), and you depend only on
the artifacts you need.

Group ID: `ie.bitstep.mango`

## Modules

| Artifact | What it is for |
|---|---|
| `mango4j-spring-aop` | Spring AOP support classes shared by the other modules |
| `mango4j-spring-data` | Spring Data helpers such as `OffsetLimitPageable` |
| `mango4j-spring-flowsink` | Annotation-based flow and step instrumentation (uses `mango4j-flowsink-core`) |
| `mango4j-spring-flowsink-boot` | Spring Boot auto-configuration for flowsink |
| `mango4j-spring-swarm` | PostgreSQL-backed, rate-limited, distributed task executor |

Naming rule across both repositories: `mango4j-*` is plain Java, `mango4j-spring-*` needs Spring.

## Using the libraries

```xml
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-spring-logging</artifactId>
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
- Docker, for the `mango4j-spring-swarm` tests (Testcontainers)

### Build

This repository depends on `mango4j` (the parent pom, `mango4j-masking`, `mango4j-flowsink-*`), so
build and install that first, then run from this directory:

```bash
mvn clean install
mvn -pl mango4j-spring-logging -am clean verify   # one module and what it needs here
```

Mutation testing with the project's thresholds: `mvn clean verify -Phammer-time`.
Publishing to Maven Central uses the `release` profile (GPG signing): `mvn deploy -Prelease`.

## Repository layout

```
mango4j-spring/
  pom.xml                  reactor: builds everything
  mango4j-spring-parent/   Spring-specific config (Boot BOM), inherits from mango4j-parent
  mango4j-spring-<module>/ the modules
  docs/                    documentation
```

## Licence

Apache License 2.0, see [LICENSE](LICENSE).
