# mango4j-spring

Code repository [*is available here*](https://github.com/bitstep-ie/mango4j-spring).

## Aim

_mango4j-spring_ aims to be a comprehensive general-purpose library for Java Spring.

## Component detection

In order for the `mango4j-spring` components (beans) to be picked up, please make sure to include `"ie.bitstep.mango.spring"` in your `ComponentScan`.

```java language=java
@ComponentScan(basePackages = {"ie.bitstep.mango.spring"})
```