# Code and security review

Reviewed the parent POM, module declarations, plugin configuration, and Checkstyle configuration location. This module contains no production Java code. This is a static review; builds were not run for this review and plugin/dependency vulnerabilities were not independently assessed.

## Finding

- **Checkstyle configuration depends on the repository layout** — `pom.xml:146` points to `${project.basedir}/../mango4j-parent/checkstyle.xml`. Child builds outside this particular sibling-directory layout, including consumers of a published parent POM, will not necessarily have that file and can fail when Checkstyle runs. Package the rules as a reusable build artifact or make the expected layout explicit and test a standalone child build.

The release signing profile is opt-in, and the Maven Central publishing plugin is inherited by child projects; release pipelines should explicitly select and verify the intended signing and publication configuration.


## Re-assessment (2026-10-31)

- **Altered:** PIT is 1.30.1 (was 1.22.0) and the `hammer-time` profile still declares 100% mutation and coverage as defaults, but four child modules (collections, reflection, utils, validation) override them with lower values. The profile therefore no longer states what the repo enforces.
- The Checkstyle sibling-directory finding was carried forward without re-verification.
