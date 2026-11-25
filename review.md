# Code and security review

The repository holds several independent Maven groups and has no root POM. Every Maven project in the repository, including the parent POMs and the swarm reference application, has its own `review.md` with its findings; none of these is a dependency vulnerability scan, and no dependency or plugin was checked against a vulnerability database.

## Cross-cutting results

- **Quality gates:** `mvn -Phammer-time clean verify` passes for every module. PIT thresholds are set per module to the current score, so a regression fails the build but the gaps below 100% are untested or unkilled code, not accepted risk. Scores (mutation / line coverage): collections 100/97, reflection 100/97, utils 98/98, validation 100/86, masking 100/100, crypto-core 99/99, crypto 99/100, flowsink-core 100/100, flowsink-spring 99/99, swarm 93/97, spring-aop 96/96, spring-logging 94/98, spring-insights 93/99, spring-data 100/95.
- **Sensitive data reaching output:** several annotations meant to protect data are silently not applied. `@Mask` on `LoggableStyle.ALL` fields (spring-logging), `@InsightVariable` masking and truncation (spring-insights), and annotation-keyed masker caching (masking) can each expose a value the developer asked to protect.
- **Cryptography:** the PBKDF2 and wrapped-key services accept CBC without authentication and take cipher parameters from stored ciphertext (crypto-core); key rotation can mark keys for deletion after failed or interrupted runs and skips encrypted entity types (crypto).
- **Concurrency and lifecycle:** thread-local diagnostic, observation and flow state is not always released on failure paths, and several static registries are mutable and unsynchronised.
- **Correctness bugs reproduced by running code:** `ObjectMutator` skipping equal objects and reused state (utils), `getEndOfMonth(int, TimeZone)` near month end (utils), URL path and query encoding (utils), `MapFormat` and `QueryParam` exceptions (utils), masker cache hash collisions and the overflow in `MaskingUtils` (masking), and the interface-proxy annotation visibility (spring-aop).

## Build notes

- PIT is `pitest-maven` 1.30.1 and Spotless 3.10.4 with palantir-java-format 2.103.0 in every tree that uses them. `-Phammer-time` runs `spotless:check` (it does not rewrite files) in flowsink and swarm.
- Parent POMs differ on the Checkstyle version (10.26.1 vs 14.3.0) and reference their Checkstyle configuration through a sibling-directory path.
