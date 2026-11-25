# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 100%, line coverage 97% (251/258); the coverage threshold is set to 97.

## Findings

1. **Callers can mutate shared cached metadata** (Medium). `ClassInfo.java:87` returns the live `accessors.values()` view, and `:150` and `:164` return the internal method lists. `MethodInfo.java:95,105` return the internal parameter-annotation list and maps, and `PropertyAccessor.java:294` returns its live annotation map. All of these are shared through the static `ReflectionUtils` cache, so one caller removing or adding an entry changes the result for every other caller. Return unmodifiable copies or views.

2. **Inherited fields resolve against the declaring class, not the supplied class** (Low). `PropertyAccessor(Class, String)` (`:33`) delegates to `PropertyAccessor(Field)`, which uses `field.getDeclaringClass()` (`:62`). When a superclass declares the field and only the subclass declares the getter or setter, the subclass accessor is not found and field access is used, bypassing any validation or transformation in that accessor. (A getter declared on the superclass and overridden in the subclass is still honoured because `Method.invoke` dispatches virtually.)

3. **The class-metadata cache retains class loaders** (Low). `ReflectionUtils.java:16` holds every inspected class and its `ClassInfo` in a static `ConcurrentHashMap` with strong keys and no eviction. Applications that load and unload classes dynamically (plugins, generated proxies) keep their class loaders reachable indefinitely. Use `ClassValue` or a weak-keyed cache.

4. **Field lookup throws `NullPointerException` for interfaces** (Low). `PropertyAccessor.getDeclaredField` (`:123`) stops recursing when the superclass is `Object`, but an interface (or primitive) has a `null` superclass, so the next recursion dereferences `null` instead of throwing `NoSuchFieldException`.

## Notes

- The force-access methods (`forceGet`, `forceSet`, `makeAccessible`) deliberately bypass Java visibility. Callers must not let untrusted input choose the class, object or field name, and module access restrictions can make them fail at runtime.
