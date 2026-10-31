# Code and security review

Reviewed the production Java classes and module configuration. This is a static review; tests were not run and third-party dependency vulnerabilities were not independently assessed.

## Findings

- **Insight variable masking and truncation settings are ignored** — `src/main/java/ie/bitstep/mango/spring/insights/internal/InsightsProcessor.java:70` converts the argument with `LoggerMapper.toLoggable` but never reads `InsightVariable.mask()` or `truncateLen()`. A caller specifying a masker or a length limit can therefore publish a value without the requested protection, including to the log publisher. Apply the configured masker and truncation before storing the value, and test annotated sensitive strings through the full publishing path.
- **Exception messages are published without sanitization** — `src/main/java/ie/bitstep/mango/spring/insights/internal/ExceptionUtils.java:16` captures each exception message, and `publisher/InsightsPublisherLogFile.java:30` serializes the resulting insight. Exceptions can contain sensitive input, URLs, or credentials, which can reach logs or storage. Apply a configurable sanitization/allowlist policy before publishing and test nested causes containing secrets.
- **Correlation ID can leak across reused threads** — `src/main/java/ie/bitstep/mango/spring/insights/initialiser/DefaultInsightsContextInitialiser.java:30` creates an MDC correlation ID if none exists, but this module never removes the value it creates. On a pooled thread without an external MDC cleanup, a subsequent unrelated operation can inherit the previous correlation ID, linking unrelated requests. Track ownership and restore the prior MDC state when the outer insight completes; test consecutive operations on the same thread.
- **Unknown HTTP status prevents insight publication** — `src/main/java/ie/bitstep/mango/spring/insights/internal/InsightWebInterceptor.java:71` uses `HttpStatus.valueOf(int)`, which throws for a valid status code not represented by the enum. The enclosing catch skips publishing all insights for that request. Use a status-code representation that accepts arbitrary valid HTTP codes and test a non-enum code.
- **Asynchronous publisher failures are not caught by the provider** — `src/main/java/ie/bitstep/mango/spring/insights/internal/InsightsProvider.java:47` catches task submission errors but exceptions thrown inside the scheduled lambda occur later on the executor thread. Depending on executor configuration, publication failures may be lost or only reach a generic handler. Catch and report errors inside each task and test a throwing publisher using a real asynchronous executor.

Request URIs are captured in insight context and may themselves contain sensitive path segments. `InsightTag` has no masking option; applications should avoid placing secrets in tags.


## Re-assessment (2026-10-31)

- No new findings; carried forward without re-verification.
- **Altered:** mutation 93% (81/87) and coverage 99%; thresholds lowered from 100 to 93/99. Six surviving mutants in a module that publishes request data and exception messages, so the masking/truncation and status-code findings above are plausibly among them.
