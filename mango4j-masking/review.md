# Code and security review

Reviewed all production Java classes, the module POM, and relevant test coverage. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Annotation cache can return a masker with different settings** — `src/main/java/ie/bitstep/mango/utils/masking/MaskerFactory.java:82` keys configured maskers by class and the annotation's 32-bit hash code rather than the annotation itself. Different annotation values can share a hash code, so the first configuration cached is reused for another. If the reused configuration reveals a longer prefix or suffix, sensitive data can appear in output. Key the cache by the annotation or an immutable key containing every configuration value, and add a collision regression test.
- **Mask configuration failures can silently fall back to defaults** — `src/main/java/ie/bitstep/mango/utils/masking/MaskerMaker.java:27` catches every exception from the annotation-taking constructor and invokes the no-argument constructor. If that constructor rejects invalid configuration or fails during initialization, the caller receives a default masker instead of the error. A less restrictive default can expose more data than intended. Fall back only when the constructor is absent and propagate constructor failures; test a throwing constructor.
- **Affix length arithmetic can overflow** — `src/main/java/ie/bitstep/mango/utils/masking/MaskingUtils.java:62` adds two nonnegative `int` affix lengths without checking overflow. With sufficiently large configured values the sum becomes negative, bypassing the short-value safeguard; the prefix branch then copies the entire input unmasked. Compare using `long` arithmetic or validate bounds before addition, and test extreme annotation and constructor values.
- **Caches accept unbounded caller-controlled keys** — `MaskerFactory.java:62` retains each distinct custom masking string and corresponding masker in a static map for the lifetime of the class loader. Applications passing untrusted or high-cardinality strings can exhaust memory. Restrict configuration to trusted bounded values or use a bounded cache.

`PanMasker` preserves the first six and last four characters, not necessarily digits; applications must ensure their input format and disclosure policy make that appropriate. `NoMasker` intentionally returns the input unchanged. The comma-separated maskers assume non-null input and use Java's `split`, which drops trailing empty elements.


## Re-assessment (2026-10-31)

- **Verified in code:** `MaskerFactory.getMasker(Mask)` keys its cache by `String.valueOf(mask.hashCode())` plus the masker class (line ~82); `MaskerMaker.make(Mask)` catches every exception from the annotation constructor and falls back to the no-arg masker; and `MaskingUtils.mask` checks `value.length() <= (prefix + suffix)` with an unchecked `int` sum while `validateAffixes` only enforces a lower bound. All three stand, and on overflow the sum goes negative so the short-value check never triggers.
- **New (low): `getMasker(Mask)` is check-then-act on a `ConcurrentHashMap`** (`containsKey` then `put`/`get`). It is harmless today because maskers are stateless, but `computeIfAbsent` would remove the window and the double lookup.
- **Note:** this module still uses the package `ie.bitstep.mango.utils`, which is the same package as `mango4j-utils`; a split package breaks if either module gains a `module-info`.
