# Code and security review

Static review of the aggregator/parent POM, its module declarations, dependency versions and build profiles. The POM has no production code. Dependencies and plugins were not scanned for known vulnerabilities.

## Findings

1. **Spotless can delete wildcard imports** (Low). `pom.xml:168` configures a `replaceRegex` step that removes `import x.*;` lines without adding explicit imports. `hammer-time` runs only `spotless:check`, which then reports any file with a wildcard import, but a manual `mvn spotless:apply` rewrites such files and leaves unresolved names.

2. **`mvn verify` does not mutation-test flowsink** (Low). `pitest.skip` is `true` by default (`pom.xml:56`) and only `-Phammer-time` sets it to `false`, so a normal build skips PIT in `flowsink-core` and `flowsink-spring`.

## Notes

- The coverage profile writes JaCoCo output to `target/site/jacoco` although the build directory is `tmp`; cleanup and CI artifact handling need to reflect that.
- Release signing is opt-in.
