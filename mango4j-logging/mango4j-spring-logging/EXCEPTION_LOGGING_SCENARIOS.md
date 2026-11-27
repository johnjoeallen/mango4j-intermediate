# Exception logging — end-to-end scenarios

Generated from `ExceptionLoggingScenariosTest` (`mango4j-spring-logging`), which writes each scenario's captured output to `target/scenario-output/`. Each scenario runs the real aspect delegate, the real `DiagnosticContextLogger` and the real level strategy; a capturing adapter records what would be logged. Timestamps and response times are fixed. Stack traces are shortened to the first frame of each exception; every exception line (`Caused by:`, `Suppressed:`) is kept.

**36 scenarios — 36 passed, 0 failed.**

Two behaviours worth knowing, both visible below:

- A child with `logException = {}` that propagates through a plain parent leaves **no log entry at all** (the child logs nothing by choice; the parent skips the same instance).
- When a parent opts in with `@OnException`, the exception is recorded twice, so the joined log *message* repeats (`card declined; card declined`).


## 1. `singleMethod_default_logsMessageCauseAndStackTrace` — ✅

**What it does:** Method with the default `logException`, throws `IllegalStateException("boom", cause "inner")`.

**Expected:** One ERROR entry. Message `boom; Cause: inner`; item has `reason` and a `stacktrace` that includes the messages and the cause chain (MESSAGE and CAUSE are selected).

**Log output:**

```
level:   ERROR
message: 'boom; Cause: inner'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom; Cause: inner",
  "stacktrace": [
    "java.lang.IllegalStateException: boom",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)",
    "Caused by: java.lang.IllegalArgumentException: inner"
  ],
  "responsetime": 0
}
```

## 2. `singleMethod_emptyDetail_logsNothing` — ✅

**What it does:** Method with `logException = {}` throws.

**Expected:** Nothing logged. The exception still propagates to the caller.

**Log output:**

```
(nothing logged)
```

## 3. `singleMethod_classNameOnly_noMessageNoStackTrace` — ✅

**What it does:** `logException = {REASON, CLASS_NAME}`, message is `boom-secret`.

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

**What it does:** `logException = {REASON, MESSAGE}`, exception has a cause.

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

**What it does:** `logException = {STACK_TRACE}`.

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

**What it does:** `logException = {STACK_TRACE}`; the exception and its cause both carry messages (`outer-LEAKA`, `inner-LEAKB`).

**Expected:** Stack trace shows the exception class and frames only: no messages and no `Caused by` line (no CAUSE selected).

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

**What it does:** `logException = {MESSAGE, STACK_TRACE}` (MESSAGE without REASON or CAUSE); exception and cause carry messages.

**Expected:** No `reason` text. The trace shows the exception message (`IllegalStateException: boom-MSG`) but no `Caused by` lines: causes need CAUSE.

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

## 8. `singleMethod_messageCauseAndStackTrace_traceHasMessagesAndCauses` — ✅

**What it does:** `logException = {MESSAGE, CAUSE, STACK_TRACE}`.

**Expected:** The trace has the messages and the cause chain (`Caused by: … IllegalArgumentException: inner-MSG`).

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
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)",
    "Caused by: java.lang.IllegalArgumentException: inner-MSG"
  ],
  "responsetime": 0
}
```

## 9. `singleMethod_causeAndStackTrace_traceHasCauseClassNamesOnly` — ✅

**What it does:** `logException = {CAUSE, STACK_TRACE}` (no MESSAGE).

**Expected:** The trace has the cause chain as class names only; no message appears anywhere.

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
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)",
    "Caused by: java.lang.IllegalArgumentException"
  ],
  "responsetime": 0
}
```

## 10. `singleMethod_classNameAndStackTrace_traceHasNoMessages` — ✅

**What it does:** `logException = {REASON, CLASS_NAME, STACK_TRACE}` (no MESSAGE).

**Expected:** `reason` is the class name and the stack trace is the exception class and frames only: no messages and no `Caused by` (no CAUSE selected).

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

## 11. `cause_reasonAndCause_causeAppendedToReason` — ✅

**What it does:** `logException = {REASON, CAUSE}` (no MESSAGE); exception `boom` with cause `inner`.

**Expected:** CAUSE alone implies the message: `reason` is `boom; Cause: inner`; no stack trace.

**Log output:**

```
level:   ERROR
message: 'boom; Cause: inner'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonAndCause.call",
  "reason": "boom; Cause: inner",
  "responsetime": 0
}
```

## 12. `cause_chainOfThreeExceptions_allCausesAppendedInOrder` — ✅

**What it does:** `{REASON, MESSAGE, CAUSE}`; exception `outer` -> cause `middle` -> cause `inner`.

**Expected:** Every cause appended in order: `outer; Cause: middle; Cause: inner`.

**Log output:**

```
level:   ERROR
message: 'outer; Cause: middle; Cause: inner'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "outer; Cause: middle; Cause: inner",
  "responsetime": 0
}
```

## 13. `cause_causeWithoutMessage_shownAsClassName` — ✅

**What it does:** `{REASON, MESSAGE, CAUSE}`; the cause has no message.

**Expected:** The cause appears as its class name: `boom; Cause: java.lang.IllegalArgumentException`.

**Log output:**

```
level:   ERROR
message: 'boom; Cause: java.lang.IllegalArgumentException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom; Cause: java.lang.IllegalArgumentException",
  "responsetime": 0
}
```

## 14. `cause_selectedButExceptionHasNoCause_nothingAppended` — ✅

**What it does:** Default detail (includes CAUSE) but the exception has no cause.

**Expected:** `reason` is just `boom`; no `Cause:` suffix and no `Caused by` line.

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

## 15. `cause_defaultDetail_reasonAndStackTraceBothShowTheCause` — ✅

**What it does:** Default detail; exception with cause `inner`.

**Expected:** Both outputs show the cause: `reason` has `; Cause: inner` and the stack trace has a `Caused by:` line with its message.

**Log output:**

```
level:   ERROR
message: 'boom; Cause: inner'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom; Cause: inner",
  "stacktrace": [
    "java.lang.IllegalStateException: boom",
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)",
    "Caused by: java.lang.IllegalArgumentException: inner"
  ],
  "responsetime": 0
}
```

## 16. `cause_deepChainInStackTrace_everyCauseHasItsOwnCausedByLine` — ✅

**What it does:** `{CAUSE, STACK_TRACE}` (no MESSAGE); three-level chain.

**Expected:** A `Caused by:` line per cause as class names (`IllegalArgumentException`, `IOException`); none of the messages appear.

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
    "at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.doLoggableMethod(AspectLoggerDelegate.java:228)",
    "Caused by: java.lang.IllegalArgumentException",
    "Caused by: java.io.IOException"
  ],
  "responsetime": 0
}
```

## 17. `singleMethod_messageAndCause_messageIncludesCause` — ✅

**What it does:** `logException = {REASON, MESSAGE, CAUSE}`.

**Expected:** Message `boom; Cause: inner`; no stack trace.

**Log output:**

```
level:   ERROR
message: 'boom; Cause: inner'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom; Cause: inner",
  "responsetime": 0
}
```

## 18. `precedence_onExceptionOverridesLogException` — ✅

**What it does:** `@OnException(IOException -> REASON, CLASS_NAME)` on a method with `logException = {REASON, MESSAGE}`; throws `IOException("disk-secret")`.

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

## 19. `precedence_onExceptionNotMatching_fallsBackToLogException` — ✅

**What it does:** Same method, but throws `IllegalStateException` (no rule matches).

**Expected:** Falls back to `logException = {REASON, MESSAGE}`: message `boom`.

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

## 20. `precedence_globalRuleOverridesLogException` — ✅

**What it does:** Global rule `IllegalStateException -> REASON, CLASS_NAME`; method has default `logException`.

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

## 21. `precedence_globalRuleWithNoDetail_logsNothing` — ✅

**What it does:** Global rule `IllegalStateException -> (nothing)`.

**Expected:** Nothing logged.

**Log output:**

```
(nothing logged)
```

## 22. `precedence_globalRuleNotMatching_fallsBackToLogException` — ✅

**What it does:** Global rule only for `IOException`; method throws `IllegalStateException`, `logException = {REASON, MESSAGE}`.

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

## 23. `precedence_onExceptionOverridesGlobalRule` — ✅

**What it does:** Global rule `IOException -> (nothing)`, but the method has `@OnException(IOException -> REASON, CLASS_NAME)`.

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

## 24. `parentPropagatesChildException_loggedOnceAtChild` — ✅

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

## 25. `parentWithMatchingOnException_loggedAtChildAndParent` — ✅

**What it does:** Same, but the parent has `@OnException(PaymentException -> REASON, MESSAGE)`.

**Expected:** Exception appears under the child and on the parent (two `reason`s).

**Log output:**

```
level:   ERROR
message: 'card declined; card declined'
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

## 26. `parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal` — ✅

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

## 27. `parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged` — ✅

**What it does:** Parent catches the child exception and throws a NEW `IllegalStateException("order failed")` with **no cause**, so the child exception is not nested in it. Child has `{REASON, MESSAGE, CAUSE}`.

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

## 28. `childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged` — ✅

**What it does:** Same, but the child has `logException = {}`.

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

## 29. `exceptionPropagatesThroughTwoParents_loggedOnce` — ✅

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

## 30. `childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll` — ✅

**What it does:** Child has `logException = {}`; parent (default detail) lets it propagate.

**Expected:** Nothing logged at all. The child logs nothing by choice and the parent skips the same instance.

**Log output:**

```
(nothing logged)
```

## 31. `childWithEmptyDetailWrappedByParent_wrapperIsLogged` — ✅

**What it does:** Child has `logException = {}`; parent wraps it in a new exception.

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

## 32. `globalRuleDoesNotMakeParentRerecordChildException` — ✅

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

## 33. `loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately` — ✅

**What it does:** Exception class is `@LoggableClass` with a `@LoggableField`; default `logException`.

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

## 34. `loggableClassException_exceptionOnly_fieldsWithoutReasonText` — ✅

**What it does:** Same exception, `logException = {EXCEPTION}`.

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

## 35. `loggableClassException_reasonOnly_textWithoutFields` — ✅

**What it does:** Same exception, `logException = {REASON, MESSAGE}`.

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

## 36. `loggableClassException_classNameOnly_fieldsAreNotLogged` — ✅

**What it does:** Same exception, `logException = {REASON, CLASS_NAME}`.

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

Other unit tests for the same functionality (assert on mocks or contexts, no log output): `DiagnosticContextExceptionDedupeTest` (20 tests, all pass), `AspectExceptionDedupeTest` (5 tests, all pass), `ExceptionDetailResolverTest` (14 tests, all pass), `ExceptionDetailStrategyTest` (8 tests, all pass), `DiagnosticContextLoggerExceptionDetailTest` (6 tests, all pass), `DiagnosticContextItemExceptionDetailTest` (19 tests, all pass).
