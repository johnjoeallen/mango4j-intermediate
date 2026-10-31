# Code and security review

Reviewed production Java classes, the module POM, and selected tests. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Aspect paths bypass configured attribute validation** — `src/main/java/ie/bitstep/mango/spring/flowsink/aspect/FlowAspect.java:91` and `:138` write extracted attributes and context directly into the current event. These web-controller and in-flow step paths do not invoke the validator used by the processor and `FlowContext`, so a value rejected on normal flow start can still reach sinks. Route all additions through a single validated API and test forbidden values in controller and nested step arguments.
- **Reactive trace context uses thread-local MDC across asynchronous boundaries** — `src/main/java/ie/bitstep/mango/spring/flowsink/webflux/TraceContextWebFilter.java:23` modifies MDC before subscription and restores it in `doFinally`, which may run on a different thread. The original thread can retain request trace values while another thread receives its saved MDC state; concurrent reactive requests can therefore be misattributed. Store trace context in Reactor context and bridge MDC only around signal handling, restoring on the same thread; test scheduler switches and concurrent requests.
- **Instrumentation failures can replace business outcomes** — `FlowAspect.java:62` calls completion inside the same try block as the business method. If completion throws after the method succeeds, it is treated as a business failure; if failure processing also throws, it can replace the original exception. Decide and implement a consistent failure policy that preserves the original business result or exception, and test failing validators and processors.

No tests were run.
- **One failing handler prevents other handlers in the same sink from receiving an event** — `src/main/java/ie/bitstep/mango/spring/flowsink/scanner/FlowSinkScanner.java:303` invokes handlers sequentially without per-handler exception isolation. An exception from one stops the loop, including later failure or completion handlers; the asynchronous bus catches the exception only after the entire compiled sink has aborted. Catch and report each handler failure independently, and test multiple handlers with the first throwing.
- **Entity detector silently accepts unscanned portions of a value** — `src/main/java/ie/bitstep/mango/spring/flowsink/validation/HibernateEntityDetector.java:79` stops at depth 50, and lines 90–112 stop after 1,000 collection elements, without rejecting the value even at ERROR level. Inaccessible fields are also skipped. A Hibernate entity beyond those bounds can pass validation and reach asynchronous sinks. Treat incomplete inspection as a distinct failure when strict validation is required, or clearly document the detector as best effort; test deep, large, and inaccessible graphs.
- **Missing trace headers leave prior MDC values active during a request** — `src/main/java/ie/bitstep/mango/spring/flowsink/AbstractTraceContextFilter.java:51` only sets values present in the new request. If the thread already has trace keys and the incoming request omits some or all headers, the old values remain in MDC until restoration, and can be attributed to the new request. Clear the managed keys before applying the resolved incoming context, then restore the saved state after the request; test requests with partial and absent headers.

Reviewed the handler scanner, entity detector, shared trace parser, and servlet trace filter. Configuration, remaining support classes, and selected tests were reviewed subsequently. No tests were run.


## Re-assessment (2026-10-31)

- **Altered:** the module moved from `ie.bitstep.mango.flowsink.spring.*` to `ie.bitstep.mango.spring.flowsink.*` (a breaking change for consumers, including references to the auto-configuration classes by name). Paths cited above are the new ones.
- **Altered:** mutation 99% (339/341) and coverage 99% (564/565); the module's PIT thresholds were lowered from 100 to 99/99. Spotless 3.10.4 reformatted six files here; formatting only.
- No new findings. The existing findings were carried forward without re-verification.
