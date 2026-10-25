# Migration guide: Spring Boot 4 / Java 25 consolidation

Goal: bring every mango4j module into this repo as a top-level directory (as the
`mango4j-spring-*` modules are now), upgrade them, then push each group to its own
GitHub repo with as few cross-module dependencies as possible.

Approach: for each external dependency, measure how much functionality it really
provides to the dependant, then either keep it as a plain versioned dependency or
break it and implement the needed part inside the dependant.

## Rules

1. Cross-group dependencies are versioned coordinates in a property, never
   `${project.version}`, so they resolve from the reactor here and from a registry
   after the split.
2. Nothing in a group references files outside its own modules once it is split.
3. Prefer inlining a small slice (one builder, a test helper) over depending on a
   whole library for it.

## Dependency analysis: `mango4j-spring-*` modules

**Status: done** (see readme log). Spring now depends on `mango4j-masking` and `mango4j-utils` (logging) and `crypto-core` (CaaS delegate) only. The
table below is the original analysis; masking was extracted into its own core module,
`mango4j-masking`, because it is reusable outside logging (API responses).

Counts are import statements of the external class in that module, from
`src/main` / `src/test`. These were the old `com.mastercard.commons.mango.*` names; they are now `ie.bitstep.mango.*`.

| Dependant | Dependency | Classes used (main / test) | Initial verdict |
|---|---|---|---|
| aop | reflection | `ReflectionUtils.getMethod` (0 / 4) | Test only. Inline as a private test helper, drop the dependency. |
| logging | reflection | `MDCTestUtils` (0 / 11) | Test only. Inline (it is an MDC clear/assert helper), drop the dependency. |
| insights | reflection | `MDCTestUtils` (0 / 7) | Test only. Share with logging via a test-jar or copy. |
| observe | reflection | `MDCTestUtils` (0 / 6) | Same. |
| logging | collections | `MapBuilder` (1 / 4), `ListBuilder` (0 / 4) | One main use. Replace with `Map.of` or a plain map and drop. |
| insights | collections | `MapBuilder` (2 / 2) | Replace in 2 main sites, drop. |
| observe | collections | `MapBuilder` (3 / 7), `ListBuilder` (0 / 10) | Replace with `Map.of` / `List.of`, drop. |
| logging | utils | masking: `Masker`, `NoMasker`, `Mask`, `MaskerFactory`; converters: `Converter`, `NoConverter`, `ConverterFactory` (about 22 main imports) | The one real runtime dependency. Part of the public annotation API (`@LoggableField(masker=...)`). Decide: move the masking and converter packages into logging, or keep `mango4j-utils`. Check what else `utils` pulls in. |
| insights | utils | `Masker`, `NoMasker` (1 / 0 each) | Follows the logging decision (insights already depends on logging). |
| logging | validation | none | Removed (it was also the source of the transitive utils dependency). |
| logging, insights, observe | `com.mastercard.enterprise:logging-rest` | none | Internal artifact, removed. It was never imported but supplied Spring, Servlet, Lombok, commons-collections and mango4j-utils transitively, so those are now declared explicitly (framework ones `provided`). |
| crypto-caas-delegate | crypto-core | `CryptoKey`, `CiphertextContainer`, `HmacHolder`, `EncryptionServiceDelegate`, `ConfigurableObjectMapperFactory` | It implements `EncryptionServiceDelegate`, so this is a true contract dependency on `mango4j-crypto`. Keep as versioned dependency. |
| root pom | `com.mastercard:parent:3.0.0`, `finicity` profile | n/a | Removed. Root pom is self-contained. JDK 23+ needed `<proc>full</proc>` for Lombok. |

Intra-group (stay together): `aop` <- `logging` <- `insights`, `observe`. `data` is a leaf.

Still to analyse when each module arrives: what `mango4j-utils` and `mango4j-crypto-core`
themselves pull in transitively, and third-party dependencies (Jackson, AspectJ,
commons-lang3, httpclient5) for Boot 4 compatibility.

## Identity (resolved)

Spring modules are now groupId `ie.bitstep.mango`, version `1.0.0-SNAPSHOT`, packages
`ie.bitstep.mango.spring.*` (crypto delegate: `ie.bitstep.mango.spring.crypto.caas.delegate`).
Imports of core and crypto-core use `ie.bitstep.mango.*`; versions come from the
`mango4j.version` and `mango4j-crypto.version` root properties.

## Open-source clean-up done

No `com.mastercard`, Finicity, Bitbucket or Confluence references remain outside this file:
internal parent, `finicity` profile, `logging-rest`, `repoTagData.json`, `ci.md`, internal
hostnames in docs, an internal package in `IgnoredPackageSanitiser`, and internal class names
and an email address in test fixtures. LICENSE (Apache-2.0) and license/scm/developers pom
metadata were added.

Still to review before publishing: `mango4j-spring-crypto-caas-delegate` talks to a CaaS
(Crypto-as-a-Service) gateway; check that API is not Mastercard-internal.

## Suggested order

1. Remove the zero-use dependencies (`validation`, `logging-rest`) and confirm it builds.
2. Break the test-only and trivial ones (`reflection`, `collections`).
3. Masking extracted into `mango4j-masking` (done); decide whether `converters` stays in utils.
4. Make the root pom self-contained (no internal parent or finicity profile).
5. Rename groupId/packages.
6. Upgrade to Java 25 and Spring Boot 4 (single `spring-boot.version`, import the
   `spring-boot-dependencies` BOM). Expect Jackson 3, Hibernate 7, Spring Framework 7
   changes, and JDK 25-capable Lombok, Mockito, Checkstyle and Pitest versions.
7. Import the other groups (`mango4j`, `mango4j-crypto`, `mango4j-swarm`,
   `mango4j-instrument` as flowsink) as top-level modules and repeat the analysis.
8. Split each group with `git subtree split` or `git filter-repo`, push, publish,
   then bump the version properties in dependants.

## Imported: core `mango4j` (upstream ad7a0e0, 2026)

`mango4j-collections`, `-reflection`, `-utils`, `-validation` and
`-hibernate-proxy-resolver` were copied from `bitstep-ie/mango4j` (no history). Upstream's
root pom became `mango4j-parent/` (modules as `../mango4j-*`, checkstyle config beside it),
and each imported module's `<parent>` got `<relativePath>../mango4j-parent/pom.xml</relativePath>`.
Repo-level files (`.github/`, `documentation/`, CHANGELOG, LICENSE, etc.) were not copied.
Build: `mvn -f mango4j-parent/pom.xml clean verify -Dgpg.skip` (passes on Java 25).
Both families are still separate: the spring modules do not yet use these.

## Imported: crypto, flowsink, swarm

Copied without history from `bitstep-ie/mango4j-crypto` (00a02bf), `mango4j-flowsink` (3f08758)
and `mango4j-swarm` (e334a03). Crypto and flowsink are kept nested
exactly as upstream: `mango4j-flowsink/` is the multi-module root holding its four
modules, and `mango4j-crypto/` is the multi-module root, with `mango4j-crypto-core`,
`mango4j-crypto/mango4j-crypto` and `mango4j-crypto-aws-kms-delegate` inside it, poms unmodified. Swarm is a single-module project, so it sits whole in `mango4j-swarm/`.
`.github/`, docs sites, LICENSE and similar repo-level files were not copied (swarm's
`docs/`, `examples/`, `scripts/` and `.redkite/` were, as part of the project).

Builds, each independent (`<dir>` is `mango4j-crypto`, `mango4j-flowsink` or `mango4j-swarm`): `mvn -f <dir>/pom.xml clean verify -Dgpg.skip`
(swarm checked with `-DskipTests`; its tests use Testcontainers).

Cross-group dependencies seen in poms: crypto-core -> `mango4j-collections`;
crypto -> `mango4j-utils`; flowsink and swarm import nothing from the core modules.

## Layout and building from the root

Each future repo is a directory pair: a parent pom directory plus its modules at the top
level. Nothing inherits from the root, which is only a throw-away aggregator.

| Future repo | Parent / root pom | Modules |
|---|---|---|
| mango4j | `mango4j-parent/` | `mango4j-collections`, `-masking`, `-reflection`, `-utils`, `-validation`, `-hibernate-proxy-resolver` |
| mango4j-crypto | `mango4j-crypto/` (nested, as upstream) | `mango4j-crypto-core`, `mango4j-crypto`, `mango4j-crypto-aws-kms-delegate` |
| mango4j-flowsink | `mango4j-flowsink/` (nested) | `mango4j-flowsink-annotations`, `-core`, `-spring`, `-spring-boot` |
| mango4j-swarm | `mango4j-swarm/` (single module) | n/a |
| mango4j-spring | `mango4j-spring-parent/` | `mango4j-spring-aop`, `-logging`, `-insights`, `-observe`, `-data`, `-crypto-caas-delegate` |

Spring modules carry their own `lombok.config`; checkstyle, docs, readme, contributing and
LICENSE live in `mango4j-spring-parent/`. When splitting, move the parent directory's
contents to the new repo root together with the modules.

`mvn clean verify -Dgpg.skip` at the root builds everything and passes, tests included.
Per group: `mvn -f <parent-dir>/pom.xml clean verify -Dgpg.skip`.

## Java 25 / Spring Boot 4 upgrade

Done: all groups build and test on JDK 25. Spring modules and swarm use Spring Boot 4.1.2
(Spring Framework 7.0.10); flowsink uses Framework 7.0.10 and `spring-boot-autoconfigure` 4.1.2.
In the spring parent, library versions now come from the `spring-boot-dependencies` BOM;
only plugins, `commons-collections4` and `logstash-logback-encoder` have explicit versions.

Changes worth knowing:
- `<proc>full</proc>` in the compiler config (JDK 23+ disables annotation processing by default).
- Surefire loads Mockito as `-javaagent` (self-attach is deprecated and fails on JDK 25).
- Boot 4 modularisation: `spring-boot-starter-data-jpa-test` for `@DataJpaTest`, new packages for
  `EntityScan`, `AutoConfigureTestDatabase`, `HibernatePropertiesCustomizer`.
- `HttpEntity` now takes `HttpHeaders`; `HttpMessageNotReadableException` needs an input message.
- Removed `system-stubs` from the CaaS delegate tests (its agent cannot attach on JDK 25); the
  test overrides the existing `SslFactory.getSystemEnv` hook instead.

Jackson 3 (`tools.jackson`, 3.1.7): done for core, crypto, spring and swarm; see the readme log
for the behaviour defaults that were restored.

Not done:
- Core and crypto have no Spring dependency; they were only moved to `java.version` 25.
- Pitest and Sonar profiles were not exercised.

## Status

Next: break `reflection`, `collections` and `utils` dependencies per the analysis table; decide
on the CaaS delegate (see above).
