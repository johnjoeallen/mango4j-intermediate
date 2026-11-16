# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 100% (94/94), line coverage 97%; PIT thresholds are set to those values.

## Findings

1. **Map overlay throws when the types conflict** (Low). `MapUtilsInternal.java:88` casts the existing target value to `Map` whenever the incoming value is a map. Overlaying a map onto a key that holds a scalar or list throws `ClassCastException`, and a present key with a `null` value throws `NullPointerException`. Define the conflict semantics and test both directions.

2. **Overlay recursion has no cycle protection** (Low). `MapUtilsInternal.overlay`/`copyTo` recurse into nested maps and lists without tracking visited objects, so a self-referencing structure ends in `StackOverflowError`.

## Notes

- `CollectionReconciler`, `MapBuilder`, `ListBuilder` and `MapUtils` were read and have no actionable finding.
