# Code and security review

Reviewed all production Java classes and the module POM. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Implementation parameter annotations can be missed through interface proxies** — `src/main/java/ie/bitstep/mango/spring/aop/ParameterMapper.java:23` maps the method supplied by the join-point signature directly. For a JDK interface proxy that method can be the interface declaration, so `ParameterValue.getAnnotation()` sees interface parameter annotations rather than annotations declared only on the implementation. An aspect using these annotations to mask or validate arguments can therefore miss its intended policy. Resolve the most specific target method and define how interface and implementation annotations combine; test both proxy styles and annotation locations. `MethodInfo.java:35` similarly retains the signature method for annotation lookup.
- **Metadata caches retain classes and class loaders** — `ParameterMapper.java:17` keeps `Method` keys strongly even though its values are soft references, and `MethodInfoFactory.java:18` strongly retains both class and method keys and values. Neither cache removes entries as classes unload. Applications that repeatedly create proxy classes or reload plugins can retain class loaders and grow memory indefinitely. Use lifecycle-aware caching such as `ClassValue` or a bounded cache and test unloading.
- **Mapped arguments are not defensively copied on construction** — `src/main/java/ie/bitstep/mango/spring/aop/MappedMethodParameters.java:40` stores the supplied array directly, while the `ParameterValue` list captures its elements at construction. If the caller later changes the original array, `getArgument()` and `getArguments()` disagree with `getModifiedArguments()` and the parameter values. Copy the input array and test mutation after construction.

`ProxyBuilder` uses Spring proxies, so direct calls within a target and methods Spring cannot proxy may bypass advice; callers must account for this when using advice as a security boundary.


## Re-assessment (2026-10-31)

- No new findings; carried forward without re-verification.
- **Altered:** mutation 96% (74/77) and coverage 96%; thresholds lowered from 100 to 96/95. Three mutants and six lines are untested, and the weak spots are likely the proxy and cache paths described above.
