# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 94% (578/614), line coverage 98%; PIT thresholds are set to 94/98. This is the largest mutation gap of the Spring modules.

## Findings

1. **`@Mask` is ignored on fields included by `LoggableStyle.ALL`** (High). `LoggerMapper.putFieldMetaData` (`:342`) resolves the masker, converter and truncation length only when the field also has `@LoggableField`. A field of an `@LoggableClass(style = ALL)` class that carries `@Mask` (or `@Converter`) alone is logged in clear text. Apply those annotations whether or not `@LoggableField` is present.

2. **A supplied masker is skipped for `@LoggableClass` values** (Medium). `LoggerMapper.toMasked` (`:201`) maps a `@LoggableClass` value with `toLoggablePojo(o, ALL)` and never applies the masker the caller passed in. Masking an object parameter with a full masker therefore exposes its fields unless each is protected on its own.

3. **Exception messages bypass the registered sanitisers** (Medium). `DiagnosticContextLogger.java:106` logs `Throwable.getMessage()` after control-character escaping only, and never calls `ExceptionSanitiserManager.getSanitisedMessage`. A sanitiser registered to strip secrets from exception messages has no effect on this path. `NullKeyException.java:5` also embeds the offending value in its message, and `LoggerMapper.toLoggable(Map)` logs that exception, so a secret is written to the log exactly when the conversion fails.

4. **Isolated logging can erase the outer diagnostic context** (Medium). `AspectLoggerDelegate.java:249` calls `DiagnosticContextThreadLocal.unset()` when the isolated context's root item finishes, which clears the whole deque, before the isolated context is popped at `:254`. The outer context is destroyed along with it.

5. **Context cleanup is skipped when a post-populator or logger throws** (Medium). `LoggingWebInterceptor.java:63-65` runs the post-populators and the logger before `DiagnosticContextThreadLocal.unset()`, with no `finally`. A failure leaves the diagnostic context on a pooled request thread for the next request.

6. **`RootLogContext` writes to top-level keys are lost** (Medium). `DiagnosticContext.getRootContext` (`:108`) returns a new context each call that shares only the root item. `RootLogContext.put` for a top-level key writes into that temporary context's `topValues` (`:218`), and the next `get` reads from yet another new context, so the value is never seen.

7. **Top-level presence checks always succeed** (Low). `DiagnosticContext.containsKey` (`:239`) returns `true` for any configured top-level key even when nothing has been stored, so callers using it to decide whether to initialise a value skip the initialisation.

8. **Global registries are mutable and unsynchronised** (Low). `ExceptionSanitiserManager.java:15` and `IgnoredPackageSanitiser.java:11` keep static `ArrayList`s with public add methods that are iterated while logging, so registration can race with logging (`ConcurrentModificationException`, lost stack traces). `LogProjectionRegistry.java:15` clears and refills a static `HashMap` on every application-context construction, and `DiagnosticContext.java:41` exposes a static mutable `topLevelKeys`. Use copy-on-write collections.

9. **The sanitiser drops every stack frame containing `$`** (Low). `IgnoredPackageSanitiser.java:82` returns true for any line with `$`, so frames from application inner classes, anonymous classes, lambdas and proxies are removed (after the first two lines), not only framework frames. Match on the package prefix.

10. **The child limit is only a log message** (Low). `DiagnosticContextItem.java:162` logs when `maxChildren` is reached and then adds the child anyway, so a long-running operation with many nested calls grows its tree without bound.

11. **The per-class logging metadata cache retains class loaders** (Low). `LoggerMapper.java:54` holds `META_DATA` in a static map keyed by `Class` with no eviction.

## Notes

- `LogUtils.toLogSafeValue` escapes newline, tab and every other non-printable character, so log forging through that path is handled.
- `IgnoredPackageSanitiser` ignores `java.base/java.util.stream.` frames, which a test runner inserts between the JUnit engine and the test method.
