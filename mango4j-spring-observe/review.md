# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 98% (174/177), line coverage 98%; PIT thresholds are set to 98/97.

## Findings

1. **The web request is kept after the request completes** (Medium). `ObservationsWebInterceptor.java:23` stores `new WebContext(request)` in the thread-local context, and `afterCompletion` does nothing. `ObservationContext.clear()` (`:67`) resets the root, observation and maps but not `webContext`, so a pooled thread keeps a reference to the finished request.

2. **An error during setup leaves the observation pushed** (Medium). `ObservationProcessor.java:46` pushes the observation before parameter and tag conversion and the entry callback, but the `try`/`finally` that pops it starts only after them. A throwing `toString()` or conversion leaves the observation on the stack.

3. **Tags bypass the logging sanitiser** (Medium). `ObservationProcessor.java:183` converts `@Tag` values with `String.valueOf(arg)`, not `LoggerMapper`, so a tagged object or secret is rendered with its raw `toString()` into parameters, logs and telemetry.

4. **Raw throwables are serialised into observation logs** (Medium). `Observation.java:101` keeps the `Throwable` with a public getter and no `@JsonIgnore`, and `LogObserver` serialises the observation, so exception messages and stack traces are written unsanitised.

5. **The correlation id is taken unvalidated and never restored** (Medium). `DefaultObservationInitializer.java:49` puts a request-supplied `X-Correlation-Id`-style header into the MDC with no length or character check, and nothing removes or restores it afterwards, so it can outlive the request on a pooled thread.

6. **`Observation.returned` is never set** (Low). `Observation.java:99` declares a JSON-serialised `returned` field that no code assigns, so it is dead and always serialises as null.

## Notes

- Observer failures are isolated: `ObserverService` catches exceptions from each observer, so a failing observer does not affect the business method.
- Observation timing uses wall-clock time, so clock adjustments affect elapsed values. Applications should keep metrics tag cardinality bounded.
