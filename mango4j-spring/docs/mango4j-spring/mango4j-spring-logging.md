# mango4j-spring-logging

## Overview

Mango4j Spring logging is a structured logging framework for building robust logging for your Spring Boot applications.

## How to use?

### Add the dependency
In order to make use of the library, you need to import the following dependency into your project.

#### Gradle Dependency

```groovy language=groovy
implementation("ie.bitstep.mango:mango4j-spring-logging:4.0.0-SNAPSHOT")
```

#### Maven Dependency

```xml language=xml
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-spring-logging</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

## What next?

* [Basic annotations](mango4j-spring-logging/annotations-basic.md): `@LoggableClass`, `@LoggableMethod` and its parameters
* [Exception logging](mango4j-spring-logging/exception-logging.md): choosing what is logged when a method throws,
  and the structure of the output
* [Custom configuration](mango4j-spring-logging/custom-config.md): the beans you can provide or override
* [Web context populators](mango4j-spring-logging/webcontext-populators.md)

Happy coding!
