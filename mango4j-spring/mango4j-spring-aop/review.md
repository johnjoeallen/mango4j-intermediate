# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 96% (74/77), line coverage 96%; PIT thresholds are set to 96/95.

## Findings

1. **Parameter annotations on the implementation are invisible through JDK interface proxies** (Medium). `ParameterMapper.java:23` maps the method returned by `methodSignature.getMethod()`. *Reproduced* with `AspectJProxyFactory`: for an interface-based (JDK) proxy the method is the interface declaration and an annotation placed on the implementation's parameter is not present (`false`), while a class-based proxy sees it (`true`). Advice that reads parameter annotations, for example to decide what to mask or redact, silently misses them. Resolve the most specific method on the target class.

2. **Metadata caches retain classes and class loaders** (Low). `ParameterMapper.java:16` keeps `Method` keys strongly (only the values are soft references) and `MethodInfoFactory.java:17` retains class and method keys and values strongly. Neither releases entries when classes unload.

3. **Mapped arguments are not defensively copied** (Low). `MappedMethodParameters.java:24` stores the supplied array directly while the `ParameterValue` list captures the elements at construction, so a later change to the caller's array makes `getArgument(i)` and `getParameterValue(i)` disagree.

## Notes

- `ProxyBuilder` uses Spring proxies, so self-invocation and methods Spring cannot proxy bypass advice; do not rely on it as a security boundary.
