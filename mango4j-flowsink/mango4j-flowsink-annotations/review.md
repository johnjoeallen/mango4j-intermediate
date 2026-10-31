# Code and security review

Reviewed all annotation definitions and the module POM. This is a static review; handler implementation and dependency vulnerabilities were not independently assessed in this module, and no tests were run.

## Finding

- **Started-handler timing contract promises synchronous execution** — `src/main/java/ie/bitstep/mango/flowsink/annotations/OnFlowStarted.java:13` says the handler runs immediately after the flow begins and before the method body. The core processor submits events to `AsyncDispatchBus`, whose workers invoke handlers asynchronously, so the annotated method can execute before the handler. Applications relying on this documented ordering for setup or security decisions can behave incorrectly. Correct the documentation to describe asynchronous best-effort delivery, or implement and test a synchronous ordering contract if that behavior is required.

`@PushAttribute` and `@PushContextValue` explicitly warn that values are forwarded without redaction. Handler scopes and lifecycle annotations are filtering mechanisms, not authorization boundaries; enforcement belongs to the consuming implementation.


## Re-assessment (2026-10-31)

- No new findings; carried forward. Re-checked this pass: the module has no Spring dependency (only `opentelemetry-api`) and no source imports `org.springframework`.
