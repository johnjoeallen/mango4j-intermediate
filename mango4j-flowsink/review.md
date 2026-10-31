# Code and security review

Reviewed the aggregator/parent POM, module declarations, dependency versions, and build profiles. This parent has no production Java code; child modules have separate reviews. No build was run and dependency/plugin vulnerabilities were not independently assessed.

## Finding

- **Mutation-test profile can remove required imports and modify source** — `pom.xml:165` configures Spotless to delete wildcard import lines, and the `hammer-time` profile runs `spotless:apply` during `process-sources`. A source file that relies on a wildcard import can be rewritten with unresolved names; running a test-oriented profile also changes the checkout. Replace deletion with a formatter that expands imports correctly, or fail a check without mutating sources, and test the profile on a file using wildcard imports.

The coverage profile directs JaCoCo output to `target/site/jacoco` even though the configured build directory is `tmp`; this is intentional in the POM and should be reflected in cleanup and CI artifact handling. Release signing is opt-in.


## Re-assessment (2026-10-31)

- **Altered:** Spotless now runs on JDK 25 (3.10.4 with palantir-java-format 2.103.0). The earlier finding about `hammer-time` rewriting sources was confirmed in practice (the first run reformatted 18 Java files across `flowsink-spring` and `swarm` before the build tested them) and is now **resolved**: `hammer-time` no longer runs `spotless:apply` in flowsink or swarm, and runs `spotless:check` at `process-sources` instead, so unformatted code fails the build and the wildcard-import removal only happens when someone runs `spotless:apply` by hand.
- **Altered:** PIT is skipped by default in this tree (`pitest.skip=true`) and only runs under `-Phammer-time`; `flowsink-core` and `flowsink-spring` also bind it to `verify` in their own poms. A plain `mvn verify` therefore does not mutation-test flowsink at all.
