# Root aggregator review

Reviewed the root Maven aggregator POM and its module declarations. It contains no production code and is documented as an unpublished intermediate reactor. No additional actionable issue was identified in this POM.

Every Maven project in this repository has its own `review.md`, including parent POMs and the reference email application. Those files describe findings and scope; they are static reviews, not independent dependency vulnerability assessments. Tests were not systematically run across the reactor. The collections test run is recorded in its module review and encountered Mockito initialization errors.


## Re-assessment (2026-10-31)

Scope of this pass: static re-assessment of each module against the current `main` (post package rename, PIT 1.30.1, Spotless 3.10.4). Findings were checked by reading the cited code; each module file says what was verified versus carried forward. Dependency/CVE scanning (e.g. OWASP dependency-check) was **not** performed anywhere.

- **Altered: tests do run.** The earlier note that tests hit Mockito initialisation errors does not reproduce. `mvn -Phammer-time clean verify` passes for every module on JDK 25, including PIT.
- **Altered: PIT is no longer a 100% gate.** Thresholds are now per module and set to each module's current score (mutation/coverage): collections 99/98, reflection 100/97, utils 98/98, validation 100/85, crypto-core 98/99, crypto 99/100, flowsink-spring 99/99, swarm 93/97, spring-aop 96/95, spring-logging 94/98, spring-insights 93/99, spring-observe 98/97, spring-data 100/95, caas-delegate 96/98. These guard against regression only; the gaps are untested code, not accepted risk.
- **Resolved:** the Spotless/palantir `NoSuchMethodError` on JDK 25 and the PIT version drift (1.22.0/1.22.1/1.25.0/1.30.1) are fixed; PIT is 1.30.1 and Spotless 3.10.4/palantir 2.103.0 throughout.
- **Highest-priority open items across the repo:** unauthenticated CBC and ciphertext-controlled parameters in crypto-core; plain-HTTP and silent trust-store fallback in the CaaS delegate; masking cache keyed by annotation hash; swarm lease/fencing races; sensitive values reaching logs (insights, observe, logging).
