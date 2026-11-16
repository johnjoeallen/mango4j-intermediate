# mango4j-spring

Code repository [*is available here*](https://github.com/bitstep-ie/mango4j). The Spring modules live in the same repository as the [core modules](mango4j-core.md) and share their version.

## Aim

_mango4j-spring_ aims to be a comprehensive general-purpose library for Java Spring.

## Modules

| Artifact | Purpose |
|---|---|
| `mango4j-spring-aop` | Spring AOP support classes |
| `mango4j-spring-logging` | Annotation-based logging, see [logging](mango4j-spring/mango4j-spring-logging.md) |
| `mango4j-spring-insights` | Annotation-based metrics, see [insights](mango4j-spring/mango4j-spring-insights.md) |
| `mango4j-spring-data` | Spring Data helpers such as `OffsetLimitPageable` |

## Component detection

In order for the `mango4j-spring` components (beans) to be picked up, please make sure to include `"ie.bitstep.mango.spring"` in your `ComponentScan`.

```java language=java
@ComponentScan(basePackages = {"ie.bitstep.mango.spring"})
```