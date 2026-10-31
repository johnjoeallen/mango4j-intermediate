# Code and security review

Reviewed the production Java classes and module POM. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Disabling processing during a flow leaves its context on the thread** — `src/main/java/ie/bitstep/mango/flowsink/core/processor/DefaultFlowProcessor.java:72` returns immediately from completion when support is disabled, without popping the event pushed at start. Re-enabling on a reused thread can attach later steps to the old flow and expose prior attributes across requests. Ensure a started flow is closed even if emission is disabled midway, and test disable/enable across pooled-thread reuse.
- **Dispatch can create an orphan worker during close** — `src/main/java/ie/bitstep/mango/flowsink/core/dispatch/AsyncDispatchBus.java:32` checks `closed` separately from worker creation. A dispatch that passes the check can create a worker after `close()` has iterated and cleared the map; that daemon worker is never shut down and its event can outlive the bus. Coordinate registration and close atomically and test their interleaving.
- **Asynchronous snapshots retain mutable values** — `src/main/java/ie/bitstep/mango/flowsink/model/FlowEvent.java:141` copies maps but not nested values, return values, or throwables; `StepEvent.snapshot()` behaves similarly. A producer or one sink can mutate shared data after dispatch, so other sinks can observe changed or inconsistent data, including sensitive values. Define an immutable event-value contract or snapshot supported nested values, and test mutation after dispatch and between sinks.
- **Flow completion ignores the supplied name** — `DefaultFlowProcessor.java:84` completes the current stack entry without checking `name`. An out-of-order or mismatched completion can close a different flow and misattribute its data. Verify the expected flow identity and define recovery for mismatches; test nested flows completed out of order.
- **Concurrent handler registration can duplicate deliveries** — `src/main/java/ie/bitstep/mango/flowsink/core/sinks/FlowHandlerRegistry.java:10` performs `contains` and `add` separately. Concurrent registrations of the same handler can both add it, causing duplicate dispatch. Use `addIfAbsent` and test concurrent registration.

The bus deliberately drops events on queue saturation and logs failures; consumers requiring durable audit records need a durable delivery mechanism. Event data and error messages should be treated as potentially sensitive by sinks.


## Re-assessment (2026-10-31)

- No new findings; carried forward without re-verification.
- Re-checked this pass: the module is Spring-free (slf4j and opentelemetry only). It holds the engine but nothing in it discovers handlers or starts flows; that is all in `flowsink-spring`. The module is at 100% mutation and coverage and keeps the full 100/100 gate.
