# Exception logging — end-to-end scenarios

Generated from `ExceptionLoggingScenariosTest` (`mango4j-spring-logging`), which writes each scenario's captured output to `target/scenario-output/`. Each scenario runs the real aspect delegate, the real `DiagnosticContextLogger` and the real level strategy; a capturing adapter records what would be logged. Timestamps and response times are fixed. Stack traces are shortened to the first frame of each exception.

**39 scenarios — 39 passed, 0 failed.**

Two behaviours worth knowing, both visible below:

- A child with `@LoggableMethod(logException = {})` that propagates through a plain parent leaves **no log entry at all** (the child logs nothing by choice; the parent skips the same instance).
- When a parent opts in with `@OnException`, the exception is recorded on both items (two `reason`s) but the joined log *message* lists it once.


## 1. `singleMethod_default_logsMessageCauseAndStackTrace` — ✅

**What it does:** A plain `@LoggableMethod` (default detail) throws `IllegalStateException("boom", cause "inner")`.

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_default_logsMessageCauseAndStackTrace(ExceptionLoggingScenariosTest.java:296)"
    ]
  },
  "responsetime": 0
}
```

## 2. `singleMethod_emptyDetail_logsNothing` — ✅

**What it does:** Method with `@LoggableMethod(logException = {})` throws.

**Expected:** Nothing logged. The exception still propagates to the caller.

**Log output:**

```
(nothing logged)
```

## 3. `singleMethod_classNameOnly_noMessageNoStackTrace` — ✅

**What it does:** `@LoggableMethod(logException = {REASON, CLASS_NAME})`, message is `boom-secret`.

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

**What it does:** `@LoggableMethod(logException = {REASON, MESSAGE})`, exception has a cause.

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

**What it does:** `@LoggableMethod(logException = {STACK_TRACE})`.

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

**What it does:** `@LoggableMethod(logException = {STACK_TRACE})`; the exception and its cause both carry messages (`outer-LEAKA`, `inner-LEAKB`).

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

**What it does:** `@LoggableMethod(logException = {MESSAGE, STACK_TRACE})` (MESSAGE without REASON or CAUSE); exception and cause carry messages.

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

**What it does:** `@LoggableMethod(logException = {MESSAGE, CAUSE, STACK_TRACE})` (no REASON).

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace(ExceptionLoggingScenariosTest.java:358)"
    ]
  },
  "responsetime": 0
}
```

## 9. `singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly` — ✅

**What it does:** `@LoggableMethod(logException = {CAUSE, STACK_TRACE})` (no MESSAGE).

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly(ExceptionLoggingScenariosTest.java:370)"
    ]
  },
  "responsetime": 0
}
```

## 10. `singleMethod_classNameAndStackTrace_traceHasNoMessages` — ✅

**What it does:** `@LoggableMethod(logException = {REASON, CLASS_NAME, STACK_TRACE})` (no MESSAGE).

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

**What it does:** `@LoggableMethod(logException = {REASON, CAUSE})` (no MESSAGE); exception `boom` with cause `inner`.

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

**What it does:** `@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})`; exception `outer` -> cause `middle` -> cause `inner`.

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

**What it does:** `@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})`; the cause has no message.

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

**What it does:** Default detail (includes CAUSE) but the exception has no cause.

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

**What it does:** Default detail; exception with cause `inner`.

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_defaultDetail_causeNodeHasItsOwnStackTrace(ExceptionLoggingScenariosTest.java:425)"
    ]
  },
  "responsetime": 0
}
```

## 16. `cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace` — ✅

**What it does:** `@LoggableMethod(logException = {CAUSE, STACK_TRACE})` (no MESSAGE); three-level chain.

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace(ExceptionLoggingScenariosTest.java:434)"
    ],
    "cause": {
      "stacktrace": [
        "java.io.IOException",
        "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace(ExceptionLoggingScenariosTest.java:434)"
      ]
    }
  },
  "responsetime": 0
}
```

## 17. `cause_depthLimit_onlyFiveNestedLevels` — ✅

**What it does:** `@LoggableMethod(logException = {REASON, CAUSE})`; a chain of eight exceptions (`level-0` … `level-7`).

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

**What it does:** `PaymentDeclinedException` (card number masked, first/last four kept) caused by `AccountLockedException` (account id masked) caused by `IOException`; default detail.

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
      "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.declinedBecauseLocked(ExceptionLoggingScenariosTest.java:467)"
    ],
    "cause": {
      "reason": "Connection reset",
      "stacktrace": [
        "java.io.IOException: Connection reset",
        "at ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.declinedBecauseLocked(ExceptionLoggingScenariosTest.java:467)"
      ]
    }
  },
  "responsetime": 0
}
```

## 19. `loggableClassChain_reasonExceptionAndCause_noStackTraces` — ✅

**What it does:** Same chain with `@LoggableMethod(logException = {REASON, EXCEPTION, CAUSE})`.

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

**What it does:** `@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})`.

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

**What it does:** `@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})` on a method with `@LoggableMethod(logException = {REASON, MESSAGE})`; throws `IOException("disk-secret")`.

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

**What it does:** Same method, but throws `IllegalStateException` (no rule matches).

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

**What it does:** Global rule `.on(IllegalStateException.class, REASON, CLASS_NAME)`; the method is a plain `@LoggableMethod` (default detail).

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

**What it does:** Global rule `.on(IllegalStateException.class)` (no detail); the method is a plain `@LoggableMethod`.

**Expected:** Nothing logged.

**Log output:**

```
(nothing logged)
```

## 25. `precedence_globalRuleNotMatching_fallsBackToLogException` — ✅

**What it does:** Global rule `.on(IOException.class, REASON, CLASS_NAME)` only; the method throws `IllegalStateException` and has `@LoggableMethod(logException = {REASON, MESSAGE})`.

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

**What it does:** Global rule `.on(IOException.class)` (no detail), but the method has `@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})`.

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

## 27. `parentPropagatesChildException_loggedOnceAtChild` — ✅

**What it does:** Parent (no `@OnException`) calls a child that throws; parent lets it propagate.

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

## 28. `parentWithMatchingOnException_exceptionNodeOnBothItemsButMessageOnlyOnce` — ✅

**What it does:** Same, but the parent has `@OnException(type = PaymentException.class, detail = {REASON, MESSAGE})`.

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

## 29. `parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal` — ✅

**What it does:** Parent catches the child exception and throws `RuntimeException("order failed", cause)`.

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

## 30. `parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged` — ✅

**What it does:** Parent catches the child exception and throws a NEW `IllegalStateException("order failed")` with **no cause**, so the child exception is not nested in it. Child has `@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})`.

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

## 31. `childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged` — ✅

**What it does:** Same, but the child has `@LoggableMethod(logException = {})`.

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

## 32. `exceptionPropagatesThroughTwoParents_loggedOnce` — ✅

**What it does:** Grandparent -> parent -> child; the child throws and both parents let it propagate.

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

## 33. `childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll` — ✅

**What it does:** Child has `@LoggableMethod(logException = {})`; parent (default detail) lets it propagate.

**Expected:** Nothing logged at all. The child logs nothing by choice and the parent skips the same instance.

**Log output:**

```
(nothing logged)
```

## 34. `childWithEmptyDetailWrappedByParent_wrapperIsLogged` — ✅

**What it does:** Child has `@LoggableMethod(logException = {})`; parent wraps it in a new exception.

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

## 35. `globalRuleDoesNotMakeParentRerecordChildException` — ✅

**What it does:** A global rule matches the exception; parent has no `@OnException`.

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

## 36. `loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately` — ✅

**What it does:** Exception class is `@LoggableClass` with a `@LoggableField`; plain `@LoggableMethod` (default detail).

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

## 37. `loggableClassException_exceptionOnly_fieldsWithoutReasonText` — ✅

**What it does:** Same exception, `@LoggableMethod(logException = {EXCEPTION})`.

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

## 38. `loggableClassException_reasonOnly_textWithoutFields` — ✅

**What it does:** Same exception, `@LoggableMethod(logException = {REASON, MESSAGE})`.

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

## 39. `loggableClassException_classNameOnly_fieldsAreNotLogged` — ✅

**What it does:** Same exception, `@LoggableMethod(logException = {REASON, CLASS_NAME})`.

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
