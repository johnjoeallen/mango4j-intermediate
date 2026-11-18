# mango4j-spring

Spring Boot support libraries. They build on the plain-Java [mango4j](https://github.com/bitstep-ie/mango4j)
modules and share its version.

Code repository [*is available here*](https://github.com/bitstep-ie/mango4j-spring).

## Modules

| Artifact | Purpose |
|---|---|
| `mango4j-spring-aop` | Spring AOP support classes |
| `mango4j-spring-logging` | Annotation-based logging, see [logging](mango4j-spring/mango4j-spring-logging.md) |
| `mango4j-spring-data` | Spring Data helpers such as `OffsetLimitPageable` |
| `mango4j-spring-flowsink` | Annotation-based flow and step instrumentation for Spring applications. The engine (`mango4j-flowsink-core`, `mango4j-flowsink-annotations`) lives in the mango4j repository |
| `mango4j-spring-flowsink-boot` | Spring Boot auto-configuration for `mango4j-spring-flowsink` |
| `mango4j-spring-swarm` | PostgreSQL-backed, rate-limited, distributed task executor. Has its own [documentation site](https://github.com/bitstep-ie/mango4j-spring/tree/main/mango4j-spring-swarm/documentation) |

## Requirements

| | Version |
|---|---|
| JDK | 25 |
| Maven | 3.9 or later |
| Spring Boot | 4.1 |

Compatibility with earlier releases is in [version compatibility](mango4j-spring/version-compatibility.md).

## Component detection

In order for the `mango4j-spring` components (beans) to be picked up, please make sure to include `"ie.bitstep.mango.spring"` in your `ComponentScan`.

```java
@ComponentScan(basePackages = {"ie.bitstep.mango.spring"})
```
