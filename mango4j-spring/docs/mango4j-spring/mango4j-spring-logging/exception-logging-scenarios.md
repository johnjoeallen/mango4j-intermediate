# Exception logging — end-to-end scenarios

*Generated from the tests, do not edit by hand. To regenerate, run `mvn -o test -Dtest=ExceptionLoggingScenariosTest` in `mango4j-logging`, then `python3 tools/generate_exception_logging_scenarios.py` in `mango4j-spring-logging`.*

Generated from `ExceptionLoggingScenariosTest` (`mango4j-spring-logging`), which writes each scenario's captured output to `target/scenario-output/`. Each scenario runs the real aspect delegate, the real `DiagnosticContextLogger` and the real level strategy; a capturing adapter records what would be logged. Each **Setup** is the annotated method(s) the scenario runs, taken from the test fixtures (a parent `void parent()` calls `child()`; a global-rules scenario shows the `ExceptionDetailRules` bean). Timestamps and response times are fixed. `stacktrace` is an object (`exception`, `message`, `frames`, and with CAUSE a nested `cause` of the same shape); frames are shortened to the first one per exception here.

**56 scenarios — 56 passed, 0 failed.**

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

**Expected:** One ERROR entry, message `boom`. `reason` is `boom`; `stacktrace` is an object (`exception`, `message`, `frames`) with the cause nested inside it as `stacktrace.cause`, which has the same shape. There is no top-level `cause`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "inner",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "singleMethod_default_logsMessageCauseAndStackTrace",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 425
        },
        {
          "…": "10 more frames"
        }
      ]
    }
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

**Expected:** Reason is `boom`; the cause and the stack trace are absent.

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

**Expected:** `stacktrace` has the exception class and its `frames` but no `message` (MESSAGE is not selected); no top-level `reason`; empty log message.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "StackTraceOnly.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
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

**Expected:** `stacktrace` has the exception class and frames only: no `message` and no nested `cause` (CAUSE is not selected). Neither message appears anywhere.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "StackTraceOnly.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
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

**Expected:** No top-level `reason`. `stacktrace.message` is `boom-MSG`, and there is no nested cause: CAUSE is not selected.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndStackTrace.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom-MSG",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
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

**Expected:** No top-level `reason` (REASON is not selected). `stacktrace` has `exception`, `message` and `frames`, with the cause nested as `stacktrace.cause` (its own `exception`, `message` and `frames`).

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageCauseAndStackTrace.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom-MSG",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "inner-MSG",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 489
        },
        {
          "…": "10 more frames"
        }
      ]
    }
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

**Expected:** The top `stacktrace` and the nested `cause` both have `exception` and `frames` but no `message`; no message appears anywhere.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "CauseAndStackTrace.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 501
        },
        {
          "…": "10 more frames"
        }
      ]
    }
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

**Expected:** `reason` is the class name; `stacktrace` has the class and frames, with no `message` and no nested cause (CAUSE is not selected).

**Log output:**

```
level:   ERROR
message: 'java.lang.IllegalStateException'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ClassNameAndStackTrace.call",
  "reason": "java.lang.IllegalStateException",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
  "responsetime": 0
}
```

## 11. `cause_reasonAndCause_causeIsNestedInTheStackTraceWithItsOwnReason` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** `reason` is `boom`. The cause is nested inside the stack trace (`stacktrace.cause`) with its `exception`, `message` (`inner`) and `frames`; there is no top-level `cause`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonCauseStackTrace.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "inner",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_reasonAndCause_causeIsNestedInTheStackTraceWithItsOwnReason",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 520
        },
        {
          "…": "10 more frames"
        }
      ]
    }
  },
  "responsetime": 0
}
```

## 12. `cause_withoutStackTrace_doesNothing` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, CAUSE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** CAUSE only adds to STACK_TRACE: without it nothing about the cause is logged. `reason` is just `boom`; there is no `stacktrace` and no `cause`, and `inner` appears nowhere.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonAndCause.call",
  "reason": "boom",
  "responsetime": 0
}
```

## 13. `cause_chainOfThreeExceptions_nestedOneInsideTheOther` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner")));
}
```

**Expected:** `reason` is outer; `stacktrace.cause.message` is middle; `stacktrace.cause.cause.message` is inner. Each cause is nested inside its parent's trace.

**Log output:**

```
level:   ERROR
message: 'outer'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "outer",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "outer",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "middle",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_chainOfThreeExceptions_nestedOneInsideTheOther",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 539
        },
        {
          "…": "10 more frames"
        }
      ],
      "cause": {
        "exception": "java.io.IOException",
        "message": "inner",
        "frames": [
          {
            "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
            "method": "cause_chainOfThreeExceptions_nestedOneInsideTheOther",
            "file": "ExceptionLoggingScenariosTest.java",
            "line": 539
          },
          {
            "…": "10 more frames"
          }
        ]
      }
    }
  },
  "responsetime": 0
}
```

## 14. `cause_causeWithoutMessage_reasonIsClassName` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException());
}
```

**Expected:** The nested cause has `exception` `java.lang.IllegalArgumentException` and no `message`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_causeWithoutMessage_reasonIsClassName",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 551
        },
        {
          "…": "10 more frames"
        }
      ]
    }
  },
  "responsetime": 0
}
```

## 15. `cause_selectedButExceptionHasNoCause_nothingAppended` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new IllegalStateException("boom");
}
```

**Expected:** `reason` is just `boom`; `stacktrace` has no `cause`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
  "responsetime": 0
}
```

## 16. `cause_defaultDetail_causeNodeHasItsOwnStackTrace` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** The top `stacktrace` has no `Caused by` entries; `stacktrace.cause` has its own `exception`, `message` (`inner`) and `frames`.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "DefaultDetail.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "inner",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_defaultDetail_causeNodeHasItsOwnStackTrace",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 566
        },
        {
          "…": "10 more frames"
        }
      ]
    }
  },
  "responsetime": 0
}
```

## 17. `cause_deepChainWithStackTraceOnly_eachCauseIsNestedInItsParentsTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner")));
}
```

**Expected:** Each level is a `cause` object inside its parent, with `exception` and `frames` but no `message`; none of the messages appear.

**Log output:**

```
level:   ERROR
message: ''
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "CauseAndStackTrace.call",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_deepChainWithStackTraceOnly_eachCauseIsNestedInItsParentsTrace",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 575
        },
        {
          "…": "10 more frames"
        }
      ],
      "cause": {
        "exception": "java.io.IOException",
        "frames": [
          {
            "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
            "method": "cause_deepChainWithStackTraceOnly_eachCauseIsNestedInItsParentsTrace",
            "file": "ExceptionLoggingScenariosTest.java",
            "line": 575
          },
          {
            "…": "10 more frames"
          }
        ]
      }
    }
  },
  "responsetime": 0
}
```

## 18. `cause_depthLimit_onlyFiveNestedLevels` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new RuntimeException("level-0", new RuntimeException("level-1", /* levels 2-6 */ new RuntimeException("level-7")));
}
```

**Expected:** The causes nest five deep (`level-1` … `level-5`); `level-6` and `level-7` are not logged.

**Log output:**

```
level:   ERROR
message: 'level-0'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "ReasonCauseStackTrace.call",
  "reason": "level-0",
  "stacktrace": {
    "exception": "java.lang.RuntimeException",
    "message": "level-0",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.RuntimeException",
      "message": "level-1",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "cause_depthLimit_onlyFiveNestedLevels",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 589
        },
        {
          "…": "10 more frames"
        }
      ],
      "cause": {
        "exception": "java.lang.RuntimeException",
        "message": "level-2",
        "frames": [
          {
            "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
            "method": "cause_depthLimit_onlyFiveNestedLevels",
            "file": "ExceptionLoggingScenariosTest.java",
            "line": 589
          },
          {
            "…": "10 more frames"
          }
        ],
        "cause": {
          "exception": "java.lang.RuntimeException",
          "message": "level-3",
          "frames": [
            {
              "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
              "method": "cause_depthLimit_onlyFiveNestedLevels",
              "file": "ExceptionLoggingScenariosTest.java",
              "line": 589
            },
            {
              "…": "10 more frames"
            }
          ],
          "cause": {
            "exception": "java.lang.RuntimeException",
            "message": "level-4",
            "frames": [
              {
                "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
                "method": "cause_depthLimit_onlyFiveNestedLevels",
                "file": "ExceptionLoggingScenariosTest.java",
                "line": 589
              },
              {
                "…": "10 more frames"
              }
            ],
            "cause": {
              "exception": "java.lang.RuntimeException",
              "message": "level-5",
              "frames": [
                {
                  "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
                  "method": "cause_depthLimit_onlyFiveNestedLevels",
                  "file": "ExceptionLoggingScenariosTest.java",
                  "line": 589
                },
                {
                  "…": "10 more frames"
                }
              ]
            }
          }
        }
      }
    }
  },
  "responsetime": 0
}
```

## 19. `loggableClassChain_default_exceptionNodeIsOnlyForTheThrownExceptionAndIsMasked` — ✅

**Setup:**

```java
@LoggableMethod
void call() {
    throw new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05",
        new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
            new IOException("Connection reset")));
}
```

**Expected:** `exception` (the fields object) is only for the thrown `PaymentDeclinedException` (`class` + fields, card masked as `4111********1111`). The causes, nested in `stacktrace.cause`, have `exception` (just the class name), `message` and `frames` but no `@LoggableField` values. The raw card number, the account number and `failedAttempts` appear nowhere.

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
  "stacktrace": {
    "exception": "ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException",
    "message": "Payment declined for order 1042",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "ie.bitstep.mango.spring.logging.test.data.AccountLockedException",
      "message": "Account locked after 5 failed attempts",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "declinedBecauseLocked",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 608
        },
        {
          "…": "10 more frames"
        }
      ],
      "cause": {
        "exception": "java.io.IOException",
        "message": "Connection reset",
        "frames": [
          {
            "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
            "method": "declinedBecauseLocked",
            "file": "ExceptionLoggingScenariosTest.java",
            "line": 608
          },
          {
            "…": "10 more frames"
          }
        ]
      }
    }
  },
  "responsetime": 0
}
```

## 20. `loggableClassException_exceptionAndCauseWithoutStackTrace_noCauseIsLogged` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, EXCEPTION, CAUSE})
void call() {
    throw new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05",
        new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
            new IOException("Connection reset")));
}
```

**Expected:** `reason` and the masked `exception` node only. CAUSE has no effect without STACK_TRACE, so no cause (and no stack trace) is logged.

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
  "responsetime": 0
}
```

## 21. `singleMethod_messageAndCause_causeIsNestedInTheStackTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IllegalStateException("boom", new IllegalArgumentException("inner"));
}
```

**Expected:** `reason` is `boom` (no `; Cause:` suffix); the cause is `stacktrace.cause` with `message` `inner` and its own frames.

**Log output:**

```
level:   ERROR
message: 'boom'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "MessageAndCause.call",
  "reason": "boom",
  "stacktrace": {
    "exception": "java.lang.IllegalStateException",
    "message": "boom",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalArgumentException",
      "message": "inner",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "singleMethod_messageAndCause_causeIsNestedInTheStackTrace",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 638
        },
        {
          "…": "10 more frames"
        }
      ]
    }
  },
  "responsetime": 0
}
```

## 22. `precedence_onExceptionOverridesLogException` — ✅

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

## 23. `precedence_onExceptionNotMatching_fallsBackToLogException` — ✅

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

## 24. `precedence_globalRuleOverridesLogException` — ✅

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

## 25. `precedence_globalRuleWithNoDetail_logsNothing` — ✅

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

## 26. `precedence_globalRuleNotMatching_fallsBackToLogException` — ✅

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

## 27. `precedence_onExceptionOverridesGlobalRule` — ✅

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

## 28. `onException_emptyDetail_logsNothingForTheMatchingType` — ✅

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

## 29. `onException_typeArray_ruleAppliesToEveryListedType` — ✅

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

## 30. `onException_defaultType_matchesAnyException` — ✅

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

## 31. `onException_repeated_firstMatchingRuleWins_broadRuleFirst` — ✅

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

## 32. `onException_repeated_firstMatchingRuleWins_specificRuleFirst` — ✅

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

## 33. `onException_ruleForASuperclass_matchesSubclasses` — ✅

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

## 34. `onException_fullDetail_reasonCauseNodeAndStackTraces` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IOException.class, detail = {REASON, MESSAGE, CAUSE, STACK_TRACE})
void call() {
    throw new IOException("disk full", new IllegalStateException("device lost"));
}
```

**Expected:** `reason`, a `stacktrace` with the `message` and `frames`, and the cause nested inside it with its own `message` and `frames`.

**Log output:**

```
level:   WARN
message: 'disk full'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionFullDetail.call",
  "reason": "disk full",
  "stacktrace": {
    "exception": "java.io.IOException",
    "message": "disk full",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ],
    "cause": {
      "exception": "java.lang.IllegalStateException",
      "message": "device lost",
      "frames": [
        {
          "class": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest",
          "method": "onException_fullDetail_reasonCauseNodeAndStackTraces",
          "file": "ExceptionLoggingScenariosTest.java",
          "line": 750
        },
        {
          "…": "10 more frames"
        }
      ]
    }
  },
  "responsetime": 0
}
```

## 35. `onException_exceptionDetail_logsMaskedLoggableClassFields` — ✅

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

## 36. `onException_differentRulesPerType_ioExceptionGetsClassNameOnly` — ✅

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

## 37. `onException_differentRulesPerType_paymentExceptionGetsMessageAndStackTrace` — ✅

**Setup:**

```java
@LoggableMethod(logException = {})
@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
@OnException(type = PaymentException.class, detail = {REASON, MESSAGE, STACK_TRACE})
void call() {
    throw new PaymentException("card declined");
}
```

**Expected:** The `PaymentException` rule applies: `reason` and a `stacktrace` with the message.

**Log output:**

```
level:   ERROR
message: 'card declined'
log:
{
  "ts": "2018-06-14T18:13:20Z",
  "method": "OnExceptionPerType.call",
  "reason": "card declined",
  "stacktrace": {
    "exception": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$PaymentException",
    "message": "card declined",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
  "responsetime": 0
}
```

## 38. `onException_overridesAnEmptyLogException_forTheMatchingType` — ✅

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

## 39. `onException_nonMatchingTypeFallsBackToAnEmptyLogException_nothingLogged` — ✅

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

## 40. `onException_classNameAndMessage_classNameThenMessage` — ✅

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

## 41. `onException_parentOptsInWithEmptyDetail_onlyTheChildShowsTheException` — ✅

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

## 42. `onException_parentOptsInWithDifferentDetail_eachItemUsesItsOwnDetail` — ✅

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

## 43. `onException_parentRuleForAnotherType_doesNotOptIn_childExceptionStaysOnTheChild` — ✅

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

## 44. `parentPropagatesChildException_loggedOnceAtChild` — ✅

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

## 45. `parentWithMatchingOnException_exceptionNodeOnBothItemsButMessageOnlyOnce` — ✅

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

## 46. `parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal` — ✅

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

## 47. `parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged` — ✅

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

@LoggableMethod(logException = {REASON, MESSAGE, CAUSE, STACK_TRACE})
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
      "stacktrace": {
        "exception": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$PaymentException",
        "message": "card declined",
        "frames": [
          {
            "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
            "method": "doLoggableMethod",
            "file": "AspectLoggerDelegate.java",
            "line": 228
          },
          {
            "…": "10 more frames"
          }
        ]
      },
      "responsetime": 0
    }
  ],
  "reason": "order failed",
  "responsetime": 0
}
```

## 48. `childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged` — ✅

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

## 49. `exceptionPropagatesThroughTwoParents_loggedOnce` — ✅

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

## 50. `childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll` — ✅

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

## 51. `childWithEmptyDetailWrappedByParent_wrapperIsLogged` — ✅

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

## 52. `globalRuleDoesNotMakeParentRerecordChildException` — ✅

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

## 53. `loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately` — ✅

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
  "stacktrace": {
    "exception": "ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest$SecretException",
    "message": "secret failure",
    "frames": [
      {
        "class": "ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate",
        "method": "doLoggableMethod",
        "file": "AspectLoggerDelegate.java",
        "line": 228
      },
      {
        "…": "10 more frames"
      }
    ]
  },
  "responsetime": 0
}
```

## 54. `loggableClassException_exceptionOnly_fieldsWithoutReasonText` — ✅

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

## 55. `loggableClassException_reasonOnly_textWithoutFields` — ✅

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

## 56. `loggableClassException_classNameOnly_fieldsAreNotLogged` — ✅

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

Other unit tests for the same functionality (assert on mocks or contexts, no log output): `DiagnosticContextExceptionDedupeTest` (20 tests, all pass), `AspectExceptionDedupeTest` (5 tests, all pass), `ExceptionDetailResolverTest` (14 tests, all pass), `ExceptionDetailStrategyTest` (8 tests, all pass), `DiagnosticContextLoggerExceptionDetailTest` (8 tests, all pass), `DiagnosticContextItemExceptionDetailTest` (26 tests, all pass).
