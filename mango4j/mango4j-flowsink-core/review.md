# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation and line coverage 100%.

## Findings

1. **A completed or disabled flow can leave its event on the thread's stack** (Medium). `DefaultFlowProcessor.java:62` returns early from `onFlowCompleted` when processing has been disabled, without popping the event pushed at start. `validate(attrs, ctx)` at `:67` and `:100` also runs before the pop, so a validator exception leaves the event pushed. Later steps on the same thread then attach to a stale flow. Pop in a `finally`.

2. **Completion ignores the supplied flow name** (Low). `DefaultFlowProcessor.java:69` and `:102` complete `support.currentContext()` without comparing it to `name`, so an out-of-order or mismatched completion closes a different flow and misattributes its data.

3. **Dispatch can create a worker after `close()`** (Low). `AsyncDispatchBus.java:37` checks `closed` and then calls `workers.computeIfAbsent`; a dispatch that passes the check can create a daemon worker after `close()` has cleared the map, and that worker is never shut down.

4. **Event snapshots are shallow** (Low). `FlowEvent.java:143` copies the top-level maps but shares nested values, the return value and the throwable, so a producer or one sink mutating them is visible to every other sink.

5. **Concurrent registration can duplicate a handler** (Low). `FlowHandlerRegistry.java:10` does `contains` then `add` on a `CopyOnWriteArrayList` as two steps. Use `addIfAbsent`.

## Notes

- The module is Spring-free (slf4j and OpenTelemetry only) and contains the engine, but nothing in it discovers handlers or starts flows; that is in `flowsink-spring`.
- The bus deliberately drops events when a sink's queue (10,000) is full and logs it; consumers that need durable audit records need a durable mechanism. Event data and error messages should be treated as potentially sensitive by sinks.
