# mango4j-spring-logging - Exception logging

When a `@LoggableMethod` throws, mango4j-spring-logging logs the exception once, at the method that threw it,
and you decide how much of it is logged: the message, the cause chain, the stack trace, the fields of your own
exception classes, or nothing at all. The output is structured so that it indexes cleanly in Splunk and
Elasticsearch and reads into a typed class.

## How it works

Out of the box a method that throws logs a `reason` (the message), the fields of the exception if its class is
a `@LoggableClass`, and the stack trace with its causes:

```java
@LoggableMethod
public Order placeOrder(OrderRequest request) {
    paymentService.charge(request.card());   // throws PaymentDeclinedException, caused by an IOException
    // ...
}
```

```json
{
  "reason": "Payment declined for order 1042",
  "exception": { "class": "com.acme.PaymentDeclinedException", "orderId": "1042", "cardNumber": "4111********1111" },
  "stacktrace": {
    "exception": "com.acme.PaymentDeclinedException",
    "message": "Payment declined for order 1042",
    "frames": [
      { "class": "com.acme.PaymentService", "method": "charge", "file": "PaymentService.java", "line": 42 }
    ],
    "cause": {
      "exception": "java.io.IOException",
      "message": "Connection reset",
      "frames": [
        { "class": "com.acme.Gateway", "method": "send", "file": "Gateway.java", "line": 17 }
      ]
    }
  }
}
```

You choose what is logged with a list of `ExceptionDetail` values. There are three places to set it, from most
to least specific: [`@OnException`](#per-exception-type-onexception) on the method, the
[global rules](#global-rules) bean, and [`@LoggableMethod(logException = ...)`](#per-method-logexception).

## Exception detail

| Value | Output key | What it logs |
|---|---|---|
| `REASON` | `reason` | Switches the text `reason` on. `CLASS_NAME` and `MESSAGE` shape its content; with neither, it is the message. |
| `CLASS_NAME` | `reason` | The exception class name, as the `reason` text. Needs `REASON`. |
| `MESSAGE` | `reason`, `stacktrace.message` | The (sanitised) message, in the `reason` text with `REASON` and as `stacktrace.message` with `STACK_TRACE`. With `CLASS_NAME` the text is `ClassName: message`. |
| `CAUSE` | `stacktrace.cause` | Nests the cause inside the stack trace object, up to 5 levels deep. Needs `STACK_TRACE`. |
| `EXCEPTION` | `exception` | The `@LoggableField` values of the exception that was thrown, with `@Mask` applied, if its class is a `@LoggableClass`. |
| `STACK_TRACE` | `stacktrace` | The stack trace as an object: `exception`, `message`, `frames` and, with `CAUSE`, `cause`. |

The default is `{ REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE }`. An empty list, `{}`, logs nothing about the
exception.

!!! note "Values that depend on others"
    `CLASS_NAME` and `MESSAGE` have no effect on `reason` without `REASON`, and `CAUSE` has no effect without
    `STACK_TRACE`: on its own it does nothing. `MESSAGE` is also what puts a `message` in the stack trace, so a
    stack trace without `MESSAGE` never contains the exception message.

### Per method: logException

```java
@LoggableMethod(logException = {})                                    // log nothing, the caller handles it
public Order lookup(long id) { ... }

@LoggableMethod(logException = { REASON, CLASS_NAME })               // the type only, the message may hold PII
public Order place(OrderRequest request) { ... }

@LoggableMethod(logException = { REASON, MESSAGE, STACK_TRACE })     // the message and the trace, no cause chain
public void reconcile() { ... }
```

### Per exception type: @OnException

`@OnException` sets the detail for particular exception types on one method. It is repeatable, and the rule with the
most specific matching type wins, whatever order the rules are written in.

```java
@LoggableMethod
@OnException(type = NotFoundException.class, detail = {})                               // nothing
@OnException(type = { IOException.class, TimeoutException.class }, detail = { REASON, CLASS_NAME })
@OnException(detail = { REASON, MESSAGE, CAUSE, STACK_TRACE })                           // everything else
public Order saveOrder(Order order) { ... }
```

* `type` is an array of exception classes, and subclasses match. It defaults to `Throwable`, so a rule without a
  `type` matches any exception, and is used only when no more specific rule matches.
* The most specific match wins: the exact class beats its superclass, which beats that class's superclass, and so on.
  Every type in a `type` array is ranked on its own. If the same type appears in two rules, the first one written is
  used.
* `detail` takes the same values as `logException`, and `{}` means nothing.
* An exception that matches no rule uses the method's `logException`.

### Global rules

Register an `ExceptionDetailStrategy` bean to set the detail once for the whole application. `ExceptionDetailRules`
builds one from rules; as with `@OnException`, the most specific matching type wins whatever order the rules are
declared in, and an exception that matches no rule falls through to the method's `logException`.

```java
@Bean
ExceptionDetailStrategy exceptionDetailStrategy() {
    return ExceptionDetailRules.builder()
        .on(NotFoundException.class)                                // no detail: log nothing
        .on(ValidationException.class, REASON, CLASS_NAME)
        .on(DataAccessException.class, REASON, MESSAGE, CAUSE, STACK_TRACE)
        .build();
}
```

You can also implement the interface yourself, to decide from the exception and the method:

```java
public interface ExceptionDetailStrategy {
    /** the detail to log, or empty to defer to the method's logException */
    Optional<Set<ExceptionDetail>> getDetail(Throwable t, MethodInfo methodInfo);
}
```

Only one `ExceptionDetailStrategy` bean is used; with none, or with more than one, there are no global rules.

### Precedence

The detail used for an exception thrown by a method is the first of these that applies:

1. a matching `@OnException` on that method
2. the global rules (`ExceptionDetailStrategy`)
3. that method's `logException`
4. the default

## Logged once, at the source

Each exception is recorded on the method that threw it. A parent `@LoggableMethod` that simply lets the very same
exception instance propagate does not record it again, so a call tree does not log one failure at every level.

```java
@LoggableMethod
public Order placeOrder(OrderRequest request) {
    paymentService.charge(request.card());     // charge() throws and logs the exception; placeOrder() adds nothing
    // ...
}
```

* A parent opts in to recording an exception it only propagates with a matching `@OnException`. It then
  uses that rule's detail, which may be `{}` (recorded, but nothing logged for it).
* A rule or global rule alone does not opt a parent in: only `@OnException` on the parent does.
* The check is on the exception instance and on the last method the parent called. A parent that catches the
  exception and throws a **new** one, with or without the original as its cause, records its own exception, and the
  child's is still logged under the child.
* When the same exception is recorded on several methods, it is shown on each of them, and the log message lists it
  once.

!!! warning "A child that logs nothing"
    If the child has `logException = {}` and a parent only propagates its exception, nothing at all is logged for
    that failure: the child logs nothing by choice, and the parent skips the same instance. Give the parent an
    `@OnException` if you want it logged there.

## Output reference

Every key has one type at every depth, so the output can be indexed and read into a typed class.

| Key | Type | Present when |
|---|---|---|
| `reason` | string | `REASON`: the class name and/or message of the exception that was thrown |
| `exception` | object | `EXCEPTION`, and the thrown exception's class is a `@LoggableClass`. Always has `class`, then the `@LoggableField` values. A field named `class` cannot override it. |
| `stacktrace` | object | `STACK_TRACE` |
| `stacktrace.exception` | string | always: the exception class |
| `stacktrace.message` | string | `MESSAGE`, and the exception has a message |
| `stacktrace.frames` | list of objects | always: `class`, `method`, and `file` and `line` when known |
| `stacktrace.cause` | object | `CAUSE`, and there is a cause. Same shape as `stacktrace`, nested at most 5 levels deep |

* A message that the JDK derived from the cause, as in `new RuntimeException(cause)`, is not shown as the
  exception's message, because it is the cause's.
* Circular causes stop at the repeat. Suppressed exceptions are not logged.
* The stack trace sanitisers (for example `IgnoredPackageSanitiser`) apply to every `frames` list.
* The top-level `exception` (the `@LoggableClass` fields) and `stacktrace.exception` (the class name) are different
  paths.

### Splunk

Ingest the events as JSON (`KV_MODE = json`, or `| spath` in the search). The fields are dotted paths:

| Field | Value |
|---|---|
| `reason` | `Payment declined for order 1042` |
| `exception.cardNumber` | `4111********1111` |
| `stacktrace.exception` | `com.acme.PaymentDeclinedException` |
| `stacktrace.frames{}.method` | `charge` |
| `stacktrace.cause.message` | `Connection reset` |
| `stacktrace.cause.frames{}.line` | `17` |

```
index=app "stacktrace.cause.exception"="java.io.IOException"
| stats count by exception.class, stacktrace.cause.message
```

### Elasticsearch and typed readers

`frames` is always a list of objects and `cause` is always a single object, so there are no mixed types to trip the
mapping. The fields of an `@LoggableClass` exception vary by class, so if two classes log the same field with
different types, map `exception` as `flattened` to avoid a mapping conflict. A typed reader needs one recursive
class:

```java
class StackTrace { String exception; String message; List<Frame> frames; StackTrace cause; }
class Frame { @JsonProperty("class") String clazz; String method; String file; Integer line; }
```

## Log level

The level comes from the `ExceptionLogLevelStrategy` (see [custom configuration](custom-config.md)), and
`forceErrorOnException = true` forces `ERROR`. With several exceptions in one entry, the highest level is used.
Exceptions whose detail is `{}` are left out of that decision.

## Moving from the boolean

`logException` used to be a `boolean`. Code that never set it keeps the default detail, but the shape of the output
changes as listed below.

!!! warning "Changes to existing output"
    * `logException = true` and `logException = false` no longer compile: use the default, or `{}`.
    * An array without `REASON` logs no `reason` text: `{ MESSAGE }` becomes `{ REASON, MESSAGE }`.
    * `stacktrace` is an object (`exception`, `message`, `frames`, `cause`), not a list of lines. There are no
      `Caused by:` or `Suppressed:` lines, and no `; Cause: ...` suffix on `reason`.
    * The fields of a `@LoggableClass` exception are under `exception` (with a `class` field), not inside `reason`,
      and only for the exception that was thrown, not its causes.
    * Exceptions are recorded on the method that threw them, so the root method no longer carries the `reason`
      and `stacktrace` of an exception that a child threw.

## Scenarios

[Exception logging scenarios](exception-logging-scenarios.md) lists the end-to-end cases, each with the annotated
method that is run, what is expected, and the log output the test captured.
