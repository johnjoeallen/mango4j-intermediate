# mango4j-spring-insights

## Overview

Mango4j spring insights is a framework for building robust insights within your Spring Boot java application.

## How to use?

## Add the dependency

#### Gradle Dependency

```groovy language=Groovy
implementation("ie.bitstep.mango.spring:mango4j-spring-insights:1.0.0")
```

#### Maven Dependency

```xml language=xml
<dependency>
    <groupId>ie.bitstep.mango.spring</groupId>
    <artifactId>mango4j-spring-insights</artifactId>
    <version>1.0.0</version>
</dependency>
```

NOTE: The default context initialiser (DefaultInsightsContextInitialiser bean) will initialise the MDC with a property "correlationid" if it does not exist, as a convention. This value will also be set on the AppInsight "correlationId" field.

NOTE: The default context initialiser (DefaultInsightsContextInitialiser bean) will set the "serviceId" field to "*unknown*".

NOTE: If you want to replace the default initialiser it is suggested that you extend it and call the initialise() method before setting any of your own default values.

Happy coding!