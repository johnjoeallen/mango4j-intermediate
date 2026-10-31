# Code and security review

Static review of the parent POM, module wiring, dependency and plugin configuration and the build-support files. The POM has no production code. Dependencies and plugins were not scanned for known vulnerabilities.

## Findings

1. **Checkstyle requires a particular sibling-directory layout** (Low). `pom.xml:173` resolves the configuration as `${project.basedir}/../mango4j-spring-parent/checkstyle.xml`, relative to each child. A build from another layout, or one using the published parent without that sibling directory, fails when Checkstyle runs. Package the configuration as a resource or use a plugin dependency.

2. **Checkstyle is on a different major version from the other parents** (Low). `pom.xml:66` uses Checkstyle 14.3.0 while `mango4j-parent` and `mango4j-crypto` use 10.26.1.

3. **The `hammer-time` profile no longer states the enforced thresholds** (Low). The profile defaults to 100%, but six child modules (aop, logging, insights, observe, data, CaaS delegate) override mutation and coverage with lower, per-module values.

## Notes

- The inherited Surefire `argLine` supplies the Mockito agent; child modules that add other agents must compose their arguments instead of replacing it.
