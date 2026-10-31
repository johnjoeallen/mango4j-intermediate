# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 99% (339/341), line coverage 99% (564/565); PIT thresholds are set to 99/99.

## Findings

1. **Aspect paths write attributes without the configured validator** (Medium). `FlowAspect.java:97` (`runWithinInterceptorFlow`) and `:138` (`handleInFlowStep`) put extracted attributes and context straight into the current event. Only `processor.onFlow*` validates, so values arriving through these two paths skip the `FlowAttributeValidator` (for example its Hibernate-entity check).

2. **Instrumentation failures can replace the business outcome** (Medium). `FlowAspect.java:61` calls `onFlowCompleted` inside the same `try` as the business method. If completion throws after the method succeeded, the `catch` treats it as a business failure and rethrows the instrumentation exception to the caller; if `onFlowFailed` throws as well, it replaces the original exception. Separate the two.

3. **One failing handler stops the rest** (Medium). `FlowSinkScanner.java:302` and `:325` invoke handlers in a loop, and `invoke` (`:403`) uses `ReflectionUtils.invokeMethod`, which rethrows. An exception from one handler prevents later handlers in the same sink, including failure and completion handlers, from receiving the event.

4. **Reactive trace context uses thread-local MDC across asynchronous boundaries** (Medium). `TraceContextWebFilter.java:26` sets MDC values before subscription and restores them in `doFinally`, which can run on a different thread. The original thread keeps the request's trace values and the other thread receives a restore for values it never set.

5. **The entity detector has blind spots** (Low). `HibernateEntityDetector.java:82` stops at depth 50 and `:92` stops after 1,000 elements per collection, both without rejecting the value (a debug log at most), and inaccessible fields are skipped. A Hibernate entity nested deeper or later than that is not detected.

## Notes

- Trace headers are bounded in length (512/128/64 characters) and parsed, so a single header cannot flood the log context.
- The package is `ie.bitstep.mango.spring.flowsink`.
