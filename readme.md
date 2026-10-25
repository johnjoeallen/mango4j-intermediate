# mango4j-intermediate

Pre-work for the open-source release of **all** mango4j modules.

This repository is a staging area, not a product. It exists so the whole mango4j family can be
brought together, upgraded to **Java 25 / Spring Boot 4 / Jackson 3**, and stripped of anything
internal, *before* each group is pushed to its own public GitHub repo. When a group is ready it is
split out (`git subtree split` or `git filter-repo`) and this repo shrinks. When the last group is
out, this repo is archived.

This readme is the running commentary: what has been done, what was learned, what is next.
Newest entries go at the bottom of the [log](#progress-log). Detail lives in
[MIGRATION.md](MIGRATION.md) (layout rules and the dependency analysis).

## Where we are

| Future repo | Directory (parent pom) | Modules | Java 25 | Boot 4 / Spring 7 | Jackson 3 | Internal refs removed | Ready to split |
|---|---|---|---|---|---|---|---|
| `mango4j` | `mango4j-parent/` | collections, reflection, utils, validation, hibernate-proxy-resolver | yes | n/a | yes | yes (upstream is public) | not yet |
| `mango4j-crypto` | `mango4j-crypto/` | crypto-core, crypto, aws-kms-delegate | yes | n/a | yes | yes (upstream is public) | not yet |
| `mango4j-flowsink` | `mango4j-flowsink/` | annotations, core, spring, spring-boot | yes | yes | n/a (no Jackson) | yes (upstream is public) | not yet |
| `mango4j-swarm` | `mango4j-swarm/` | single module | yes | yes | yes | yes (upstream is public) | not yet |
| `mango4j-spring` | `mango4j-spring-parent/` | aop, logging, insights, observe, data, crypto-caas-delegate | yes | yes | yes | yes, one open question | not yet |

"Ready to split" stays "not yet" until the cross-group dependencies are broken and each group
builds standalone from a fresh clone (see [Next](#next)).

```bash
mvn clean verify -Dgpg.skip                              # build and test everything
mvn -f mango4j-spring-parent/pom.xml clean verify -Dgpg.skip   # one group, as its future repo will build
```

Nothing inherits from the root pom. It is a throw-away aggregator; every group is a standalone
Maven project.

## Progress log

### 2026-10-25 start: bringing the family together
- Started from `mango4j-spring` only (an internal, Mastercard-hosted codebase with no history here).
- Imported the public repos from `bitstep-ie` without history, each in its upstream layout:
  `mango4j` (core), `mango4j-crypto`, `mango4j-flowsink`, `mango4j-swarm`. Core's parent pom now
  lives in `mango4j-parent/`; crypto and flowsink stay nested as upstream has them.
- Wrote the dependency analysis of the spring modules against core and crypto
  ([MIGRATION.md](MIGRATION.md)). Headline: most core usage in spring is test-only helpers.

### Removing everything internal
- The spring modules inherited from an internal parent pom, used an internal artifact
  (`logging-rest`), an internal Artifactory profile, and had Bitbucket/Confluence links in docs.
  All gone. The root and spring parent are self-contained.
- Lesson: `logging-rest` looked unused (never imported) but was quietly supplying Spring, Servlet,
  Lombok and `mango4j-utils` transitively. Removing it meant declaring those explicitly.
- Renamed the spring modules to groupId `ie.bitstep.mango`, packages `ie.bitstep.mango.spring.*`,
  to match the public modules. Scrubbed internal class names and an email from test fixtures.
- Added Apache-2.0 LICENSE and pom metadata (licenses, scm, developers).
- Open question: `mango4j-spring-crypto-caas-delegate` talks to a CaaS gateway. We still need to
  confirm that API is not internal before publishing that module.

### Java 25 and Spring Boot 4
- Spring modules and swarm are on Spring Boot 4.1.2 (Spring Framework 7.0.10), flowsink on
  Framework 7.0.10; all groups build and test on JDK 25.
- Library versions in the spring parent now come from the Boot BOM instead of per-library
  properties.
- Things that bit us:
  - JDK 23+ disables annotation processing by default, so Lombok silently did nothing
    (`<proc>full</proc>` fixes it).
  - Mockito self-attach fails on JDK 25; surefire now loads it as a `-javaagent`.
  - `system-stubs` cannot attach its agent on JDK 25. The one test that used it now overrides an
    existing hook instead, and the dependency is gone.
  - Boot 4 split Boot into modules and moved packages (`@DataJpaTest`, `EntityScan`,
    `HibernatePropertiesCustomizer`); Spring 7 deprecated the `HttpEntity` multi-value-map
    constructor.
- The spring modules had been inheriting from the root pom. Moved that into
  `mango4j-spring-parent/` so every group is independent, matching the others.

### Jackson 3
- Moved core, crypto, spring and swarm from Jackson 2 (`com.fasterxml.jackson.databind`) to
  Jackson 3 (`tools.jackson`). Annotations keep their old package. The Java time and Jdk8 modules
  are built in now, so those dependencies are gone. Version pinned to 3.1.7 to match Boot 4.1.2.
- Jackson 3 changed defaults. We restored the old behaviour where it was observable:
  - empty beans no longer fail serialization, but our error handling relies on it
    (`FAIL_ON_EMPTY_BEANS` re-enabled);
  - properties are sorted alphabetically now, which changes JSON field order in logs and
    messages (`SORT_PROPERTIES_ALPHABETICALLY` disabled);
  - `ObjectMapper` is immutable, so mappers are built with `JsonMapper.builder()` and the crypto
    mapper factory configures field visibility in the builder;
  - `convertValue` throws `JacksonException` rather than `IllegalArgumentException`;
  - `TextNode` is `StringNode`, `JsonProcessingException` is `JacksonException` (unchecked).
- Core's parent used to add `jackson-databind` to every core module; it is now declared only in
  `mango4j-utils`, plus test scope in `mango4j-collections`.

## Next
1. Break cross-group dependencies per the [analysis](MIGRATION.md): inline the test-only
   `reflection` and `collections` helpers in the spring modules, and decide whether spring keeps
   `mango4j-utils` (masking and converters) or takes those classes in.
2. Confirm the CaaS delegate is publishable.
3. Make each group build standalone from a clean clone against published (or `-SNAPSHOT`)
   artifacts of the others, not the reactor.
4. Per-group open-source hygiene: README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, `.github`
   workflows, changelog (the spring one is still the old internal history).
5. Split each group into its own repo, publish, then bump versions in dependants.
