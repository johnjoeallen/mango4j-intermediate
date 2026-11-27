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

* logException, default `{ REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE }`

  What to log when this method throws. An array of `ExceptionDetail`. Each concern has its own output key:

  | Value | Output key | Logs |
  |---|---|---|
  | `REASON` | `reason` | switches the text reason on. `CLASS_NAME` and `MESSAGE` shape its content; with neither it is the message |
  | `CLASS_NAME` | `reason` | the exception class name |
  | `MESSAGE` | `reason` | the (sanitised) message |
  | `CAUSE` | `stacktrace` | adds the cause to the stack trace: the cause is an object at the end of its parent's `stacktrace` array, `{"cause": {"reason": ..., "stacktrace": [..., {"cause": ...}]}}`, up to 5 levels deep. Its `reason` follows `REASON`. It only adds to `STACK_TRACE`: on its own, or without `STACK_TRACE`, it does nothing |
  | `EXCEPTION` | `exception` | for the exception that was thrown, if its class is annotated `@LoggableClass`: `{"class": "com.acme.PaymentException", "code": "X-42"}`, its `@LoggableField`s with `@Mask` applied. A `@LoggableField` named `class` cannot override `class`. Nothing for other exceptions, and nothing for causes |
  | `STACK_TRACE` | `stacktrace` | the exception's own stack trace as a list of lines (the exception line, then its frames). The message is on the exception line only with `MESSAGE`. There are no `Caused by:` lines: with `CAUSE` the cause is nested at the end of the array as an object |

  `CLASS_NAME` and `MESSAGE` have no effect without `REASON`, and `CAUSE` has no effect without `STACK_TRACE`. `{}` logs nothing about the exception
  (use it when the caller or a global handler logs it). Class name and message together render as `ClassName: message`.

  Example with the default detail, for `PaymentDeclinedException` (a `@LoggableClass`) caused by an `IOException`:

  ```json
  {
    "reason": "Payment declined for order 1042",
    "exception": { "class": "com.acme.PaymentDeclinedException", "orderId": "1042", "cardNumber": "4111********1111" },
    "stacktrace": [
      "com.acme.PaymentDeclinedException: Payment declined for order 1042",
      "at com.acme.PaymentService.charge(PaymentService.java:42)",
      { "cause": {
          "reason": "Connection reset",
          "stacktrace": ["java.io.IOException: Connection reset", "at com.acme.Gateway.send(Gateway.java:17)"]
      } }
    ]
  }
  ```

  `stacktrace` is a list of lines followed, with `CAUSE`, by at most one object per level, so a consumer
  that reads it as lines should keep to the string elements. In Splunk the lines are `stacktrace{}` and the
  cause fields are `stacktrace{}.cause.reason`, `stacktrace{}.cause.stacktrace{}`, and so on.

* forceErrorOnException default false

  If set to true will force log level to ERROR

#### OnException

When a `@LoggableMethod` throws, the exception is recorded against the method that threw it. A parent
`@LoggableMethod` that simply lets the same exception instance propagate does **not** record it again,
so it is logged once, at its source.

A parent opts in to recording it as well with `@OnException`:

```java language=java
@LoggableMethod
@OnException(type = PaymentException.class, detail = { ExceptionDetail.REASON, ExceptionDetail.MESSAGE })
public Order placeOrder(OrderRequest r) {
    paymentService.charge(r.card());   // throws PaymentException
    ...
}
```

* `type` is an array of exception classes (subclasses match). Default is `Throwable`, i.e. any exception.
* `@OnException` is repeatable; rules are checked in declaration order and the first match wins.
* A wrapper exception (a new instance with the child's exception as its cause) is a different exception
  and is always recorded by the parent.
* `detail` takes the same values as `logException` and overrides it for the exceptions the rule matches.
  Exceptions not matched by any `@OnException` use the method's `logException`.

#### Global exception rules

Register an `ExceptionDetailStrategy` bean to set exception detail once for the whole application:

```java language=java
@Bean
ExceptionDetailStrategy exceptionDetailStrategy() {
    return ExceptionDetailRules.builder()
        .on(NotFoundException.class)                          // log nothing
        .on(ValidationException.class, REASON, CLASS_NAME)
        .on(DataAccessException.class, REASON, MESSAGE, CAUSE)
        .build();
}
```

Rules are checked in order and the first matching type (subclasses included) wins. What gets used for an exception:

1. a matching `@OnException` on the method that threw it
2. the global rules
3. that method's `logException`
4. the default (`REASON`, `MESSAGE`, `CAUSE`, `EXCEPTION`, `STACK_TRACE`)

An exception matching no global rule falls through to `logException`. Global rules do not make a parent
re-record a child's exception; only a matching `@OnException` does that.

<!--name=admonition;type=info;title=NOTE;body=
Behaviour changes: (1) exceptions are now recorded on the method that threw them rather than on the root
method, so the root's top-level reason/stacktrace is no longer present for propagated exceptions;
(2) the structured fields of a `@LoggableClass` exception are logged under `exception` (with a `class` field) instead of inside `reason`,
and `reason` is now always text; (3) causes are objects nested at the end of the `stacktrace` array (at most 5 deep), not a `; Cause: ...` suffix on `reason` or `Caused by:` lines.
-->

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
