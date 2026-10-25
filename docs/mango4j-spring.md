# mango4j-spring

Code repository [*is available here*](https://scm.mastercard.int/stash/projects/MANGO/repos/mango4j-spring/browse).

## Aim

_mango4j-spring_ aims to be a comprehensive general-purpose library for Java Spring.

## Component detection

In order for the `mango4j-spring` components (beans) to be picked up, please make sure to include `"com.mastercard.commons.mango"` in your `ComponentScan`.

```java language=java
@ComponentScan(basePackages = {"com.mastercard.commons.mango"})
```