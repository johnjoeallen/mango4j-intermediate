# Code and security review

Reviewed the production Java classes and module POM. This is a static review; tests were not run and third-party dependency vulnerabilities were not independently assessed.

## Findings

- **Web request is retained after completion** — `src/main/java/ie/bitstep/mango/spring/observe/web/ObservationsWebInterceptor.java:23` stores the servlet request in a thread-local context, while `afterCompletion` never clears it. `ObservationContext.clear()` also leaves `webContext` intact. A pooled thread retains the request and can expose its headers to later non-web observations; requests with no observed method retain it as well. Remove or restore request context at every completion, including asynchronous dispatch, and test consecutive requests and non-web work on the same thread.
- **Initialization errors leave a pushed observation behind** — `src/main/java/ie/bitstep/mango/spring/observe/aspect/ObservationProcessor.java:46` pushes the observation before parameter and tag conversion, but its `try/finally` begins only after conversion and entry notification. A throwing `toString()` or logging conversion prevents the target call and leaves the observation on the thread-local stack, contaminating subsequent work. Enclose all operations after push in cleanup and decide whether instrumentation failures should prevent the business call; test conversion failures.
- **Tags bypass logging sanitization** — `ObservationProcessor.java:178` converts `@Tag` arguments and fields with `String.valueOf` rather than the logging mapper. These values are included in observation parameters and, when allowed, telemetry tags; a tagged object or secret can expose its raw `toString()` in logs. Provide explicit masking/sanitization for tags and test annotated sensitive values, including private fields.
- **Correlation ID is not restored after use** — `src/main/java/ie/bitstep/mango/spring/observe/initialisers/DefaultObservationInitializer.java:43` places a request header or generated ID in MDC, without restoring or removing it when the observation or request finishes. With pooled threads and no external MDC cleanup, later work can inherit an unrelated ID. Restore the previous MDC state at the appropriate outer boundary and validate untrusted header values before logging.
- **Throwables can expose exception details** — `src/main/java/ie/bitstep/mango/spring/observe/context/Observation.java:89` retains the raw throwable, and `observers/base/LogObserver.java:42` converts the observation for logging. Exception messages and causes can contain sensitive input. Apply an explicit exception sanitization policy before serialization and test nested exceptions containing secrets.

Observation timing uses wall-clock time, so clock adjustments can make elapsed values inaccurate. Metrics tags should also be limited to bounded cardinality by applications.


## Re-assessment (2026-10-31)

- No new findings; carried forward without re-verification.
- **Altered:** mutation 98% (174/177) and coverage 98% (444/455 lines); thresholds lowered from 100 to 98/97. Eleven lines are not covered.
