# mango4j-spring

## Overview

Mango4j Spring Edition is a framework designed to support common activities required by developers in a Spring Boot application. It is a modular framework, which provides developers with flexibility in choosing the specific components they need based on their project requirements.

Three are currently three modules available:

- mango4j-spring-logging - Annotation based logging
- mango4j-spring-insights - Annotation based metrics
- mango4j-spring-aop - SpringAOP support classes
- mango4j-spring-observe - Generalised annotation based observability


## Using the libraries

See the project site and reference documentation to get started building an application using each specific module of this framework.
[Usage documentation based on version](https://confluence.mastercard.int/display/MANGO/mango4j-spring)

## Contributions

All contributions are welcome, and we encourage everyone to check the [contributions.md](contributing.md) for some guidelines.


## Development



### Pre-requisites

Your local setup is required to have installed:

- JDK 17
- Git
- Your favourite java IDE (we know IntelliJ works :))
- Maven (3.8.1)

And access to artifactory.

### Get started

#### Clone
Clone the repository locally using your favourite git client and the https link from bitbucket
```
https://scm.mastercard.int/stash/scm/mango/mango4j-spring.git
```

#### Build

```bash
mvn clean install
```

#### Run pit mutations

```bash
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
```