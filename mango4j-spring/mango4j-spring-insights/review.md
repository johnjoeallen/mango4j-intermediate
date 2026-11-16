# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 93% (81/87), line coverage 99%; PIT thresholds are set to 93/99.

## Findings

1. **`@InsightVariable` masking and truncation are ignored** (High). `InsightVariable.java:16,18` define `mask()` and `truncateLen()`, but `InsightsProcessor.java:74` converts the argument with `LoggerMapper.toLoggable(arg)` and never reads either. A caller who marks a variable with a masker still publishes the raw value, and the length limit is not applied.

2. **Exception messages are published unsanitised** (Medium). `ExceptionUtils.java:16` copies each exception's message into the insight and `InsightsPublisherLogFile` serialises it. Messages can contain user input, URLs or credentials, and none of the logging module's sanitisers are applied.

3. **The correlation id is created but never removed** (Medium). `DefaultInsightsContextInitialiser.java:31` puts a generated id into the MDC when none exists and nothing in this module removes it, so on a pooled thread without external MDC cleanup it carries over to unrelated work.

4. **An unknown HTTP status prevents publication** (Medium). `InsightWebInterceptor.java:71` uses `HttpStatus.valueOf(int)`, which throws for a code that is not an enum constant; the enclosing `catch` then skips publishing every insight for that request. Use `HttpStatusCode.valueOf`.

5. **Asynchronous publisher failures are not handled** (Low). `InsightsProvider.java:47` catches only submission errors, so an exception thrown inside the task on the executor thread is handled by that executor's policy.

## Notes

- Request URIs are captured in the insight context and may contain sensitive path segments. `InsightTag` has no masking option, so secrets must not be placed in tags.
