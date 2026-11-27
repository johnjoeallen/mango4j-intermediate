# mango4j-spring-logging - Basic Annotating

!!! warning "Spring AOP"
    This framework relies on Spring AOP, therefore any method calls need to cross service boundaries or be called via a
    `WithAOP<>proxy()` in order to be intercepted by Spring AOP.

## Annotate loggable classes

```java
@Service
@LoggableClass
public class FunService {
```

!!! note
    This works on any Java class.

## Annotate loggable methods

```java
@LoggableMethod
public void funMethod() {
```

!!! note
    This works on any type of method with the exception of private methods.

### LoggableMethod parameters

* isolated, default false

    Causes a log message on return from this method even if it's nested inside an existing @LoggableMethod

* tracking, default TrackingType.TRACK
    * TRACK, Track the flow by keeping the current tracking cookie (correlation id)
    * DONT_TRACK, Don't track the flow (clear the correlation id)
    * SPLIT, Split from the flow (new correlation id)
    * TRAIL, Create a trail from the flow to a new flow (previous correlation id + new correlation id)

* logReturn, default false

    If set to true will log the response from the method

* logException, default `{ REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE }`

    What to log when this method throws, as an array of `ExceptionDetail` values. `{}` logs nothing about the
    exception. `@OnException` and global rules can override it per exception type. See
    [exception logging](exception-logging.md) for the values, the output, and how an exception is logged once, at the
    method that threw it.

    ```java
    @LoggableMethod(logException = { ExceptionDetail.REASON, ExceptionDetail.CLASS_NAME })
    public void funMethodThatThrows() {
    ```

    !!! warning "Changed from a boolean"
        `logException = true` and `logException = false` no longer compile. See
        [moving from the boolean](exception-logging.md#moving-from-the-boolean).

* forceErrorOnException default false

    If set to true will force log level to ERROR

### Examples

Logs the response from the method:

```java
@LoggableMethod(logReturn = true)
public int funMethodWithReturn() {
```

Loggable parameters into methods:

```java
@LoggableMethod(logReturn = true)
public int funMethodWithParametersAndResponse(@LoggableParam(name = "hiParam") String serviceParam) {
```

## Customisation & Configuration

See [custom configuration](custom-config.md) for the beans you can provide or override: the logger adapter, the
exception log level, and the exception detail rules.
