# Code and security review

Static review of all annotation definitions and the POM. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** passes the default 100% mutation and coverage thresholds.

## Findings

1. **`@OnFlowStarted` documents synchronous behaviour that is not provided** (Low). `OnFlowStarted.java:12` says the method runs immediately after the flow begins and before the annotated method body. `DefaultFlowProcessor` hands the event to `AsyncDispatchBus`, whose per-sink worker thread invokes handlers asynchronously, so the handler can run after the body or after the flow ends. Correct the Javadoc, or document that ordering is not guaranteed.

## Notes

- The module has no Spring dependency (only `opentelemetry-api`).
- `@PushAttribute` and `@PushContextValue` warn that values are forwarded without redaction. Scope and lifecycle annotations filter delivery; they are not authorisation boundaries.
