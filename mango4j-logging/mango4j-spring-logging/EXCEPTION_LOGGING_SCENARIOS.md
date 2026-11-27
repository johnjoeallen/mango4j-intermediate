# Exception logging — end-to-end scenarios

Generated from `ExceptionLoggingScenariosTest` (`mango4j-spring-logging`), which writes each scenario's captured output to `target/scenario-output/`. Each scenario runs the real aspect delegate, the real `DiagnosticContextLogger` and the real level strategy; a capturing adapter records what would be logged. Each **Setup** is the annotated method(s) the scenario runs, taken from the test fixtures (a parent `void parent()` calls `child()`; a global-rules scenario shows the `ExceptionDetailRules` bean). Timestamps and response times are fixed. Stack traces are shortened to the first frame of each exception.

**55 scenarios — 55 passed, 0 failed.**

Two behaviours worth knowing, both visible below:

- A child with `@LoggableMethod(logException = {})` that propagates through a plain parent leaves **no log entry at all** (the child logs nothing by choice; the parent skips the same instance).
- When a parent opts in with `@OnException`, the exception is recorded on both items (two `reason`s) but the joined log *message* lists it once.


## 1. `singleMethod_default_logsMessageCauseAndStackTrace` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** One ERROR entry, message `boom`. The item has `reason`, `stacktrace` and a nested `cause` node with its own `reason` (`inner`) and `stacktrace`. No `Caused by:` lines.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": [
    "java.lang.IllegalStateException: boom",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "reason": "inner",
    "stacktrace": [
      "java.lang.IllegalArgumentException: inner",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_default_logsMessageCauseAndStackTrace(ExceptionLoggingScenariosTest.java:397)"
    ]
  },
  "responsetime": 0
}
```

## 2. `singleMethod_emptyDetail_logsNothing` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Nothing logged. The exception still propagates to the caller.

**Log output:**

```
(nothing logged)
```

## 3. `singleMethod_classNameOnly_noMessageNoStackTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CLASS_NAME})
void call() {
    throw new IllegalStateException("boom-secret");
}
```

**Expected:** Reason is the class name only; message text and stack trace are absent.

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ClassNameOnly.call",
  "reason": "java.lang.IllegalStateException",
  "responsetime": 0
}
```

## 4. `singleMethod_messageOnly_noCauseNoStackTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** Reason is `boom`; cause and stack trace are absent.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "reason": "boom",
  "responsetime": 0
}
```

## 5. `singleMethod_stackTraceOnly_stackTraceWithoutReason` — ✅

**Setup:**

```java
@LoggableMethod(logException = {STACK_TRACE})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Stack trace present with class names only (no MESSAGE selected); no `reason`; empty log message.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "StackTraceOnly.call",
  "stacktrace": [
    "java.lang.IllegalStateException",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 6. `singleMethod_stackTraceOnly_neverLeaksMessagesFromTheExceptionOrItsCauses` — ✅

**Setup:**

```java
@LoggableMethod(logException = {STACK_TRACE})
void call() {
    throw new IllegalStateException("outer-LEAKA", new IllegalArgumentException("inner-LEAKB"));
}
```

**Expected:** Stack trace shows the exception class and frames only: no messages and no `cause` node (no CAUSE selected).

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "StackTraceOnly.call",
  "stacktrace": [
    "java.lang.IllegalStateException",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 7. `singleMethod_messageAndStackTrace_messageShownInTraceNotAsReason` — ✅

**Setup:**

```java
@LoggableMethod(logException = {MESSAGE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG"));
}
```

**Expected:** No `reason` text. The trace shows the exception message (`IllegalStateException: boom-MSG`) but there is no `cause` node: causes need CAUSE.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndStackTrace.call",
  "stacktrace": [
    "java.lang.IllegalStateException: boom-MSG",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 8. `singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG"));
}
```

**Expected:** No `reason` anywhere. The top `stacktrace` has the message on its first line, and the nested `cause` node has its own `stacktrace` with the cause message. No `Caused by:` lines.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageCauseAndStackTrace.call",
  "stacktrace": [
    "java.lang.IllegalStateException: boom-MSG",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "stacktrace": [
      "java.lang.IllegalArgumentException: inner-MSG",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace(ExceptionLoggingScenariosTest.java:459)"
    ]
  },
  "responsetime": 0
}
```

## 9. `singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly` — ✅

**Setup:**

```java
@LoggableMethod(logException = {CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG"));
}
```

**Expected:** Both `stacktrace`s (top and in the `cause` node) show class names only; no message appears anywhere.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "CauseAndStackTrace.call",
  "stacktrace": [
    "java.lang.IllegalStateException",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "stacktrace": [
      "java.lang.IllegalArgumentException",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly(ExceptionLoggingScenariosTest.java:471)"
    ]
  },
  "responsetime": 0
}
```

## 10. `singleMethod_classNameAndStackTrace_traceHasNoMessages` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CLASS_NAME, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG"));
}
```

**Expected:** `reason` is the class name and the stack trace is the exception class and frames only: no messages and no `cause` node (no CAUSE selected).

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ClassNameAndStackTrace.call",
  "reason": "java.lang.IllegalStateException",
  "stacktrace": [
    "java.lang.IllegalStateException",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 11. `cause_reasonAndCause_causeIsANestedNodeWithItsOwnReason` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CAUSE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** `reason` is `boom`; `cause` is a nested node with `reason` `inner`; no stack traces.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonAndCause.call",
  "reason": "boom",
  "cause": {
    "reason": "inner"
  },
  "responsetime": 0
}
```

## 12. `cause_chainOfThreeExceptions_nestedOneInsideTheOther` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})
void call() {
    throw new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner")));
}
```

**Expected:** Nested one inside the other: `reason` outer, `cause.reason` middle, `cause.cause.reason` inner.

**Log output:**

```
level:   ERROR
message: 'outer'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "outer",
  "cause": {
    "reason": "middle",
    "cause": {
      "reason": "inner"
    }
  },
  "responsetime": 0
}
```

## 13. `cause_causeWithoutMessage_reasonIsClassName` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException());
}
```

**Expected:** The `cause` node's `reason` is its class name, `java.lang.IllegalArgumentException`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom",
  "cause": {
    "reason": "java.lang.IllegalArgumentException"
  },
  "responsetime": 0
}
```

## 14. `cause_selectedButExceptionHasNoCause_nothingAppended` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** `reason` is just `boom`; no `cause` node and no `Caused by` line.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": [
    "java.lang.IllegalStateException: boom",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 15. `cause_defaultDetail_causeNodeHasItsOwnStackTrace` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** The top `stacktrace` has no `Caused by:` line; the `cause` node has its own `stacktrace` starting `java.lang.IllegalArgumentException: inner`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": [
    "java.lang.IllegalStateException: boom",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "reason": "inner",
    "stacktrace": [
      "java.lang.IllegalArgumentException: inner",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_defaultDetail_causeNodeHasItsOwnStackTrace(ExceptionLoggingScenariosTest.java:526)"
    ]
  },
  "responsetime": 0
}
```

## 16. `cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner")));
}
```

**Expected:** Each level is a nested `cause` node with its own class-name-only `stacktrace`; none of the messages appear.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "CauseAndStackTrace.call",
  "stacktrace": [
    "java.lang.IllegalStateException",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "stacktrace": [
      "java.lang.IllegalArgumentException",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace(ExceptionLoggingScenariosTest.java:535)"
    ],
    "cause": {
      "stacktrace": [
        "java.io.IOException",
        "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace(ExceptionLoggingScenariosTest.java:535)"
      ]
    }
  },
  "responsetime": 0
}
```

## 17. `cause_depthLimit_onlyFiveNestedLevels` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CAUSE})
void call() {
    throw new RuntimeException("level-0", new RuntimeException("level-1", /* levels 2-6 */ new RuntimeException("level-7")));
}
```

**Expected:** The `cause` nodes nest five deep (`level-1` … `level-5`); `level-6` and `level-7` are not logged.

**Log output:**

```
level:   ERROR
message: 'level-0'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonAndCause.call",
  "reason": "level-0",
  "cause": {
    "reason": "level-1",
    "cause": {
      "reason": "level-2",
      "cause": {
        "reason": "level-3",
        "cause": {
          "reason": "level-4",
          "cause": {
            "reason": "level-5"
          }
        }
      }
    }
  },
  "responsetime": 0
}
```

## 18. `loggableClassChain_default_eachNodeHasItsOwnMaskedExceptionFields` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05",
        new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
            new IOException("Connection reset")));
}
```

**Expected:** Each `@LoggableClass` exception has its own `exception` node (`class` + fields) — card `4111********1111`, masked account id, `failedAttempts` — inside the `cause` nesting; the `IOException` has text and trace only. The raw card and account numbers appear nowhere.

**Log output:**

```
level:   ERROR
message: 'Payment declined for order 1042'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "Payment declined for order 1042",
  "exception": {
    "class": "ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException",
    "orderId": "1042",
    "cardNumber": "4111********1111",
    "declineCode": "05"
  },
  "stacktrace": [
    "ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException: Payment declined for order 1042",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "reason": "Account locked after 5 failed attempts",
    "exception": {
      "class": "ie.bitstep.mango.spring.logging.test.data.AccountLockedException",
      "accountId": "ACC-7XXXXXXX",
      "failedAttempts": 5
    },
    "stacktrace": [
      "ie.bitstep.mango.spring.logging.test.data.AccountLockedException: Account locked after 5 failed attempts",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.declinedBecauseLocked(ExceptionLoggingScenariosTest.java:568)"
    ],
    "cause": {
      "reason": "Connection reset",
      "stacktrace": [
        "java.io.IOException: Connection reset",
        "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.declinedBecauseLocked(ExceptionLoggingScenariosTest.java:568)"
      ]
    }
  },
  "responsetime": 0
}
```

## 19. `loggableClassChain_reasonExceptionAndCause_noStackTraces` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, EXCEPTION, CAUSE})
void call() {
    throw new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05",
        new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
            new IOException("Connection reset")));
}
```

**Expected:** The compact form: `reason`, `exception` and `cause` nodes only, no `stacktrace`; masking still applied.

**Log output:**

```
level:   ERROR
message: 'Payment declined for order 1042'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonExceptionCause.call",
  "reason": "Payment declined for order 1042",
  "exception": {
    "class": "ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException",
    "orderId": "1042",
    "cardNumber": "4111********1111",
    "declineCode": "05"
  },
  "cause": {
    "reason": "Account locked after 5 failed attempts",
    "exception": {
      "class": "ie.bitstep.mango.spring.logging.test.data.AccountLockedException",
      "accountId": "ACC-7XXXXXXX",
      "failedAttempts": 5
    },
    "cause": {
      "reason": "Connection reset"
    }
  },
  "responsetime": 0
}
```

## 20. `singleMethod_messageAndCause_causeIsANestedNode` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** `reason` is `boom` (no `; Cause:` suffix); the cause is a nested `cause` node with `reason` `inner`; no stack traces.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom",
  "cause": {
    "reason": "inner"
  },
  "responsetime": 0
}
```

## 21. `precedence_onExceptionOverridesLogException` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
void call() {
    throw new IOException("disk-secret");
}
```

**Expected:** Class name only; the message text is absent.

**Log output:**

```
level:   WARN
message: 'java.io.IOException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionClassNameForIo.call",
  "reason": "java.io.IOException",
  "responsetime": 0
}
```

## 22. `precedence_onExceptionNotMatching_fallsBackToLogException` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Falls back to `@LoggableMethod(logException = {REASON, MESSAGE})`: message `boom`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionClassNameForIo.call",
  "reason": "boom",
  "responsetime": 0
}
```

## 23. `precedence_globalRuleOverridesLogException` — ✅

**Setup:**

```java
// ExceptionDetailStrategy bean (global rules)
ExceptionDetailRules.builder()
    .on(IllegalStateException.class, REASON, CLASS_NAME)
    .build();

@LoggableMethod
void call() {
    throw new IllegalStateException("boom-secret");
}
```

**Expected:** Class name only; message and stack trace absent.

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "java.lang.IllegalStateException",
  "responsetime": 0
}
```

## 24. `precedence_globalRuleWithNoDetail_logsNothing` — ✅

**Setup:**

```java
// ExceptionDetailStrategy bean (global rules)
ExceptionDetailRules.builder()
    .on(IllegalStateException.class)
    .build();

@LoggableMethod
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Nothing logged.

**Log output:**

```
(nothing logged)
```

## 25. `precedence_globalRuleNotMatching_fallsBackToLogException` — ✅

**Setup:**

```java
// ExceptionDetailStrategy bean (global rules)
ExceptionDetailRules.builder()
    .on(IOException.class, REASON, CLASS_NAME)
    .build();

@LoggableMethod(logException = {REASON, MESSAGE})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Falls back to the method: message `boom`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "reason": "boom",
  "responsetime": 0
}
```

## 26. `precedence_onExceptionOverridesGlobalRule` — ✅

**Setup:**

```java
// ExceptionDetailStrategy bean (global rules)
ExceptionDetailRules.builder()
    .on(IOException.class)
    .build();

@LoggableMethod(logException = {REASON, MESSAGE})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
void call() {
    throw new IOException("disk");
}
```

**Expected:** `@OnException` wins: class name is logged.

**Log output:**

```
level:   WARN
message: 'java.io.IOException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionClassNameForIo.call",
  "reason": "java.io.IOException",
  "responsetime": 0
}
```

## 27. `onException_emptyDetail_logsNothingForTheMatchingType` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = IllegalStateException.class, detail = {})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Nothing logged: the rule's empty detail applies to `IllegalStateException`.

**Log output:**

```
(nothing logged)
```

## 28. `onException_typeArray_ruleAppliesToEveryListedType` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = {IOException.class, IllegalStateException.class}, detail = {REASON, CLASS_NAME})
void call() {
    throw new IllegalStateException("boom-MSG");
}
```

**Expected:** Class name only (`java.lang.IllegalStateException`); `boom-MSG` and the stack trace are absent. The `type` array lists two types and either one matches.

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionTypeArray.call",
  "reason": "java.lang.IllegalStateException",
  "responsetime": 0
}
```

## 29. `onException_defaultType_matchesAnyException` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(detail = {REASON, MESSAGE})
void call() {
    throw new IOException("disk full");
}
```

**Expected:** `type` defaults to `Throwable`, so the rule matches `IOException`: `reason` is `disk full`, overriding `logException = {}`.

**Log output:**

```
level:   WARN
message: 'disk full'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionCatchAll.call",
  "reason": "disk full",
  "responsetime": 0
}
```

## 30. `onException_repeated_firstMatchingRuleWins_broadRuleFirst` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = RuntimeException.class, detail = {REASON, CLASS_NAME})
@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
void call() {
    throw new IllegalStateException("boom-MSG");
}
```

**Expected:** Rules are checked in declaration order: the broad `RuntimeException` rule comes first, so only the class name is logged.

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionBroadRuleFirst.call",
  "reason": "java.lang.IllegalStateException",
  "responsetime": 0
}
```

## 31. `onException_repeated_firstMatchingRuleWins_specificRuleFirst` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
@OnException(type = RuntimeException.class, detail = {REASON, CLASS_NAME})
void call() {
    throw new IllegalStateException("boom-MSG");
}
```

**Expected:** The specific `IllegalStateException` rule comes first, so the message `boom-MSG` is logged.

**Log output:**

```
level:   ERROR
message: 'boom-MSG'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionSpecificRuleFirst.call",
  "reason": "boom-MSG",
  "responsetime": 0
}
```

## 32. `onException_ruleForASuperclass_matchesSubclasses` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = RuntimeException.class, detail = {REASON, MESSAGE})
void call() {
    throw new PaymentException("card declined");
}
```

**Expected:** `PaymentException` is a subclass of `RuntimeException`, so the rule matches: message `card declined`.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionSuperclassRule.call",
  "reason": "card declined",
  "responsetime": 0
}
```

## 33. `onException_fullDetail_reasonCauseNodeAndStackTraces` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IOException.class, detail = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IOException("disk full", new IllegalStateException("device lost"));
}
```

**Expected:** `reason`, a stack trace starting with the message, and a nested `cause` node with its own `reason` and stack trace.

**Log output:**

```
level:   WARN
message: 'disk full'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionFullDetail.call",
  "reason": "disk full",
  "stacktrace": [
    "java.io.IOException: disk full",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "cause": {
    "reason": "device lost",
    "stacktrace": [
      "java.lang.IllegalStateException: device lost",
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.onException_fullDetail_reasonCauseNodeAndStackTraces(ExceptionLoggingScenariosTest.java:710)"
    ]
  },
  "responsetime": 0
}
```

## 34. `onException_exceptionDetail_logsMaskedLoggableClassFields` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = PaymentDeclinedException.class, detail = {REASON, EXCEPTION})
void call() {
    throw new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05", null);
}
```

**Expected:** `reason` and an `exception` node with the masked card `4111********1111`; no stack trace.

**Log output:**

```
level:   ERROR
message: 'Payment declined for order 1042'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionLoggableClass.call",
  "reason": "Payment declined for order 1042",
  "exception": {
    "class": "ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException",
    "orderId": "1042",
    "cardNumber": "4111********1111",
    "declineCode": "05"
  },
  "responsetime": 0
}
```

## 35. `onException_differentRulesPerType_ioExceptionGetsClassNameOnly` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
@OnException(type = PaymentException.class, detail = {REASON, MESSAGE, STACK_TRACE})
void call() {
    throw new IOException("disk-MSG");
}
```

**Expected:** The `IOException` rule applies: class name only.

**Log output:**

```
level:   WARN
message: 'java.io.IOException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionPerType.call",
  "reason": "java.io.IOException",
  "responsetime": 0
}
```

## 36. `onException_differentRulesPerType_paymentExceptionGetsMessageAndStackTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
@OnException(type = PaymentException.class, detail = {REASON, MESSAGE, STACK_TRACE})
void call() {
    throw new PaymentException("card declined");
}
```

**Expected:** The `PaymentException` rule applies: `reason` and a stack trace with the message.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionPerType.call",
  "reason": "card declined",
  "stacktrace": [
    "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$PaymentException: card declined",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 37. `onException_overridesAnEmptyLogException_forTheMatchingType` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** `@OnException` overrides `logException = {}` for `IllegalStateException`: the message `boom` is logged.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionOverEmptyLogException.call",
  "reason": "boom",
  "responsetime": 0
}
```

## 38. `onException_nonMatchingTypeFallsBackToAnEmptyLogException_nothingLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
void call() {
    throw new IOException("disk");
}
```

**Expected:** No rule matches `IOException`, so the method's `logException = {}` applies: nothing logged.

**Log output:**

```
(nothing logged)
```

## 39. `onException_classNameAndMessage_classNameThenMessage` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = IllegalStateException.class, detail = {REASON, CLASS_NAME, MESSAGE})
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** `reason` is `java.lang.IllegalStateException: boom`.

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException: boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionClassNameAndMessage.call",
  "reason": "java.lang.IllegalStateException: boom",
  "responsetime": 0
}
```

## 40. `onException_parentOptsInWithEmptyDetail_onlyTheChildShowsTheException` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = PaymentException.class, detail = {})
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** The parent opts in (its rule matches) but its detail is empty, so it logs nothing for the exception; only the child shows it (one `reason`).

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionParentEmptyDetail.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "responsetime": 0
}
```

## 41. `onException_parentOptsInWithDifferentDetail_eachItemUsesItsOwnDetail` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = PaymentException.class, detail = {REASON, CLASS_NAME})
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** Each item uses its own detail: the parent's `reason` is the class name, the child's is the message.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionParentClassName.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "reason": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$PaymentException",
  "responsetime": 0
}
```

## 42. `onException_parentRuleForAnotherType_doesNotOptIn_childExceptionStaysOnTheChild` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = IOException.class, detail = {REASON, MESSAGE})
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** The parent's rule is for `IOException`, so it does not opt in: the exception appears only under the child.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionParentOtherType.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "responsetime": 0
}
```

## 43. `parentPropagatesChildException_loggedOnceAtChild` — ✅

**Setup:**

```java
@LoggableMethod
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new IllegalStateException("boom");
}
```

**Expected:** One entry; the exception appears once, under the child (`called`). The parent has no `reason`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "boom",
      "responsetime": 0
    }
  ],
  "responsetime": 0
}
```

## 44. `parentWithMatchingOnException_exceptionNodeOnBothItemsButMessageOnlyOnce` — ✅

**Setup:**

```java
@LoggableMethod
@OnException(type = PaymentException.class, detail = {REASON, MESSAGE})
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** The exception is recorded on the child and on the parent (two `reason`s), but the log message holds it once: `card declined`.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionMessageForPayment.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "reason": "card declined",
  "responsetime": 0
}
```

## 45. `parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void parent() {
    try {
        child();
    } catch (RuntimeException e) {
        throw new RuntimeException("order failed", e);
    }
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** Wrapper is a different instance, so the parent logs it as well as the child (two `reason`s).

**Log output:**

```
level:   ERROR
message: 'card declined; order failed'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "called": [
    {
      "method": "MessageOnly.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "reason": "order failed",
  "responsetime": 0
}
```

## 46. `parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void parent() {
    try {
        child();
    } catch (RuntimeException e) {
        throw new IllegalStateException("order failed");
    }
}

@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})
void child() {
    throw new PaymentException("card declined");
}
```

**Expected:** Child exception is still logged under the child (`called`), and the parent logs its own exception. Two `reason`s; message `card declined; order failed` (child first).

**Log output:**

```
level:   ERROR
message: 'card declined; order failed'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "called": [
    {
      "method": "MessageAndCause.call",
      "reason": "card declined",
      "responsetime": 0
    }
  ],
  "reason": "order failed",
  "responsetime": 0
}
```

## 47. `childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void parent() {
    try {
        child();
    } catch (RuntimeException e) {
        throw new IllegalStateException("order failed");
    }
}

@LoggableMethod(logException = {})
void child() {
    throw new PaymentException("card-secret");
}
```

**Expected:** Only the parent's new exception is logged; the child exception (`card-secret`) appears nowhere.

**Log output:**

```
level:   ERROR
message: 'order failed'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "called": [
    {
      "method": "NoDetail.call",
      "responsetime": 0
    }
  ],
  "reason": "order failed",
  "responsetime": 0
}
```

## 48. `exceptionPropagatesThroughTwoParents_loggedOnce` — ✅

**Setup:**

```java
@LoggableMethod
void grandparent() {
    parent();
}

@LoggableMethod
void parent() {
    child();
}

@LoggableMethod(logException = {REASON, MESSAGE})
void child() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Logged once, at the child.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "called": [
    {
      "method": "DefaultDetail.call",
      "called": [
        {
          "method": "MessageOnly.call",
          "reason": "boom",
          "responsetime": 0
        }
      ],
      "responsetime": 0
    }
  ],
  "responsetime": 0
}
```

## 49. `childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll` — ✅

**Setup:**

```java
@LoggableMethod
void parent() {
    child();
}

@LoggableMethod(logException = {})
void child() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Nothing logged at all. The child logs nothing by choice and the parent skips the same instance.

**Log output:**

```
(nothing logged)
```

## 50. `childWithEmptyDetailWrappedByParent_wrapperIsLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void parent() {
    try {
        child();
    } catch (RuntimeException e) {
        throw new RuntimeException("outer failed", e);
    }
}

@LoggableMethod(logException = {})
void child() {
    throw new IllegalStateException("inner-boom");
}
```

**Expected:** The parent logs the wrapper (message `outer failed`).

**Log output:**

```
level:   ERROR
message: 'outer failed'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "called": [
    {
      "method": "NoDetail.call",
      "responsetime": 0
    }
  ],
  "reason": "outer failed",
  "responsetime": 0
}
```

## 51. `globalRuleDoesNotMakeParentRerecordChildException` — ✅

**Setup:**

```java
// ExceptionDetailStrategy bean (global rules)
ExceptionDetailRules.builder()
    .on(IllegalStateException.class, REASON, MESSAGE)
    .build();

@LoggableMethod
void parent() {
    child();
}

@LoggableMethod
void child() {
    throw new IllegalStateException("boom");
}
```

**Expected:** Still logged once: only `@OnException` makes a parent re-record.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "called": [
    {
      "method": "DefaultDetail.call",
      "reason": "boom",
      "responsetime": 0
    }
  ],
  "responsetime": 0
}
```

## 52. `loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new SecretException("X-42");
}
```

**Expected:** Two separate keys: `reason` is the text (`secret failure`) and `exception` is an object with a `class` field (the exception class name) plus the structured fields (`code: X-42`).

**Log output:**

```
level:   ERROR
message: 'secret failure'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "secret failure",
  "exception": {
    "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException",
    "code": "X-42"
  },
  "stacktrace": [
    "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException: secret failure",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)"
  ],
  "responsetime": 0
}
```

## 53. `loggableClassException_exceptionOnly_fieldsWithoutReasonText` — ✅

**Setup:**

```java
@LoggableMethod(logException = {EXCEPTION})
void call() {
    throw new SecretException("X-42");
}
```

**Expected:** Only the `exception` key (with `X-42`); no `reason` text and no stack trace.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ExceptionFieldsOnly.call",
  "exception": {
    "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException",
    "code": "X-42"
  },
  "responsetime": 0
}
```

## 54. `loggableClassException_reasonOnly_textWithoutFields` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE})
void call() {
    throw new SecretException("X-42");
}
```

**Expected:** Only the `reason` text; no `exception` key and `X-42` appears nowhere.

**Log output:**

```
level:   ERROR
message: 'secret failure'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageOnly.call",
  "reason": "secret failure",
  "responsetime": 0
}
```

## 55. `loggableClassException_classNameOnly_fieldsAreNotLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CLASS_NAME})
void call() {
    throw new SecretException("X-42");
}
```

**Expected:** `reason` is the class name; no `exception` key; `X-42` appears nowhere.

**Log output:**

```
level:   ERROR
message: 'ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ClassNameOnly.call",
  "reason": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException",
  "responsetime": 0
}
```

---

Other unit tests for the same functionality (assert on mocks or contexts, no log output): `DiagnosticContextExceptionDedupeTest` (20 tests, all pass), `AspectExceptionDedupeTest` (5 tests, all pass), `ExceptionDetailResolverTest` (14 tests, all pass), `ExceptionDetailStrategyTest` (8 tests, all pass), `DiagnosticContextLoggerExceptionDetailTest` (8 tests, all pass), `DiagnosticContextItemExceptionDetailTest` (25 tests, all pass).
