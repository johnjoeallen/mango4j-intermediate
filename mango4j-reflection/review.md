# Code and security review

Reviewed all production Java classes, the module POM, and the public reflection interfaces. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Callers can mutate shared cached metadata** — `src/main/java/ie/bitstep/mango/reflection/utils/ClassInfo.java:83` exposes the live `accessors.values()` view, and lines 145 and 159 return internal method lists. `src/main/java/ie/bitstep/mango/reflection/utils/MethodInfo.java:102` similarly returns mutable parameter-annotation maps and lists; `src/main/java/ie/bitstep/mango/reflection/accessors/PropertyAccessor.java:299` exposes the annotation map. These objects are shared through the static `ReflectionUtils` cache. A caller can remove metadata or annotations for later callers, and concurrent modifications can produce inconsistent results or exceptions. In consumers that use annotations to control masking or other policy, that corruption may affect security decisions. Return immutable snapshots or unmodifiable deeply immutable views and test attempted mutations.
- **Inherited properties can bypass subclass accessors** — `PropertyAccessor.java:32` resolves a named inherited field and constructs metadata using the field's declaring class, rather than the supplied subclass. A subclass override of a getter or setter is therefore not discovered when the superclass has no matching method; field access is used instead. If an override enforces validation or transforms a value, `setField` or `forceSetField` can bypass that behavior. Preserve the requested class for accessor lookup and test inherited fields with subclass accessors. Relatedly, `ClassInfo.java:62` only indexes declared methods and fields, so inherited metadata is not included in class scans.
- **Metadata cache retains class loaders** — `src/main/java/ie/bitstep/mango/reflection/utils/ReflectionUtils.java:16` keeps strong references to every inspected class and its metadata in a static map with no eviction. Applications dynamically loading and unloading plugins or generated classes can retain class loaders indefinitely and exhaust memory. Consider `ClassValue` or another lifecycle-aware cache and test class-loader reclamation.

The force-access methods intentionally bypass Java visibility and can read or write private state; callers must not expose unrestricted class, object, or field selection to untrusted input. Java module access restrictions can also cause these methods to fail at runtime.


## Re-assessment (2026-10-31)

- No new findings. Existing findings were carried forward without re-verification.
- **Altered:** coverage is 97% (251/258 lines) and its PIT coverage threshold was lowered from 100 to 97 to match. Seven uncovered lines remain; the cached-metadata and inherited-accessor findings are likely among the untested paths.
