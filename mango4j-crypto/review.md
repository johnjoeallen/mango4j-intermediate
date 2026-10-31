# Code and security review

Reviewed the aggregator/parent POM, its module declarations, inherited dependencies and plugins, and build support files. This parent contains no production Java code. Reviews of the three child modules are in their respective `review.md` files.

No additional actionable issue was identified in the parent configuration by this static review. The release profile makes signing opt-in, and publication is configured via the Central publishing plugin; release automation should explicitly select and verify its signing settings. No build was run and dependency/plugin vulnerabilities were not independently assessed.


## Re-assessment (2026-10-31)

- **Altered:** PIT is now 1.30.1 (was 1.22.1) and the hammer-time profile still demands 100% at the aggregator level; the per-module thresholds for `mango4j-crypto-core` (98/99) and `mango4j-crypto` (99/100) override it. Nothing else changed.
