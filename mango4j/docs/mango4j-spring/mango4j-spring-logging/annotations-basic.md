# mango4j-spring-logging - Basic Annotating

WARNING: This framework relies on Spring AOP, therefore:
Any method calls need to cross service boundaries or be called via a WithAOP<>proxy() in order to be intercepted by Spring AOP.

### Annotate loggable classes

```java language=java
@Service
@LoggableClass
public class FunService {
```

<!--name=admonition;type=info;title=NOTE;body=
This works on any Java class.
-->

### Annotate loggable methods


<!--name=admonition;type=info;title=NOTE;body=
This works on any type of method with the exception of private methods.
-->


```java language=java
@LoggableMethod
public void funMethod() {
```

#### LoggableMethod parameters

* isolated, default false

  Causes a log message on return from this method even if it's nested inside an existing @LoggableMethod

* tracking, default TrackingType.TRACK
 ** TRACK, Track the flow by keeping the current tracking cookie (correlation id)
 ** DONT_TRACK, Don't track the flow (clear the correlation id)
 ** SPLIT, Split from the flow (new correlation id)
 ** TRAIL, Create a trail from the flow to a new flow (previous correlation id + new correlation id)

* logReturn, default false

  If set to true will log the response from the method

* logException default true

  If set to false will supress the logging of exceptions

* forceErrorOnException default false

  If set to true will force log level to ERROR

<!--name=admonition;type=info;title=NOTE;body=
Example, logs the response from the method.
-->


```java language=java
@LoggableMethod(logReturn = true)
public int funMethodWithReturn() {
```


<!--name=admonition;type=info;title=NOTE;body=
Loggable parameters into methods
-->


```java language=java
@LoggableMethod(logReturn = true)
public int funMethodWithParametersAndResponse(@LoggableParam(name = "hiParam") String serviceParam) {
```


## Customisation & Configuration
