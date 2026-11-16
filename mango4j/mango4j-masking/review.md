# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 100%, line coverage 100%.

## Findings

1. **Different `@Mask` settings can share one cached masker, leaking more of a value than configured** (High). `MaskerFactory.java:84` caches parameterised maskers under the masker class and `String.valueOf(mask.hashCode())`, not the annotation itself. Annotation hash codes collide easily. *Reproduced:* `@Mask` (mask everything) and `@Mask(prefix=1, postfix=1)` have the same hash (`-1540334681`); after the second is resolved, the first returns `S******9` for `SECRET99`. Whichever configuration is resolved first is used for the other, so a field meant to be fully masked can reveal its first and last characters. Key the cache on the annotation's values (or the annotation instance).

2. **A failing masker constructor silently falls back to a default** (Medium). `MaskerMaker.java:30` catches every `Exception` from the `Mask`-taking constructor and falls back to the no-argument constructor. A masker that rejects an invalid configuration is replaced by its default instance, which may reveal more, and the caller never sees the error. Catch only `NoSuchMethodException`.

3. **Prefix/suffix length arithmetic can overflow** (Low). `MaskingUtils.java:59` tests `value.length() <= (maskPrefixLength + maskSuffixLength)` in `int`, and `validateAffixes` only checks a lower bound. *Reproduced:* `mask("secret-value", "*", Integer.MAX_VALUE, 1)` returns the value unmasked. Reachable through `@Mask(prefix=..., postfix=...)` values chosen by the developer. Use `long` arithmetic or validate an upper bound.

4. **Comma-separated maskers change the list shape and reject null** (Low). `AbstractCommaSeparatedListMasker.java:31` uses `String.split(",")`, which drops trailing empty elements. *Reproduced:* `"a,b,,"` masks to `X,X`. A `null` input throws `NullPointerException`, unlike `MaskingUtils.mask`, which handles empty input.

5. **Masker caches have no bound** (Low). `MaskerFactory.java:64` keeps one masker per distinct custom masking-character string for the life of the class loader. This is bounded for annotation-driven use but not for callers of `getMasker(Class, String)` that pass runtime values.

## Notes

- `PanMasker` keeps the first six and last four characters, which are not necessarily digits; the caller must make sure that disclosure is acceptable for the input format. `NoMasker` returns its input unchanged by design.
- Masked output has the same length as the input, which reveals the length.
- The module's package is `ie.bitstep.mango.utils`, the same as `mango4j-utils`; two jars sharing a package would conflict if either gains a `module-info`.
