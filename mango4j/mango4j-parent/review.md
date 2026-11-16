# Code and security review

Static review of the parent POM, module declarations, plugin configuration and the Checkstyle setup. The POM has no production code. Dependencies and plugins were not scanned for known vulnerabilities.

## Findings

1. **Checkstyle requires a particular sibling-directory layout** (Low). `pom.xml:152` resolves the configuration as `${project.basedir}/../mango4j-parent/checkstyle.xml`, relative to each child, so consumers of a published parent POM do not have the file and the Checkstyle step fails.

2. **Publishing is configured without signing** (Low). The `central-publishing-maven-plugin` (`pom.xml:91`) is part of the default build, while GPG signing exists only in the opt-in `release` profile. A deploy without `-Prelease` attempts an unsigned publish.

3. **The `hammer-time` profile no longer states the enforced thresholds** (Low). The profile and its comment say it fails below 100%, but `collections`, `reflection`, `utils` and `validation` override mutation and coverage with lower, per-module values.
