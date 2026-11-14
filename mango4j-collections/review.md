# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 99% (137/138), line coverage 98%; PIT thresholds are set to those values.

## Findings

1. **Replacing a keyed entry never closes the old value** (Medium). `ConcurrentCache.java:180` calls `cache.put(...)` and discards the previous entry, so it is never moved to the evicted set. An `AutoCloseable` value (for example a cached encryption key holder) that is replaced by `put` is not closed by the cache, not even by `shutdown()`, and the key material stays in memory until garbage collection. Move the replaced entry to delayed eviction (`put` returns the old value from the map) and test replacement and shutdown.

2. **`putCurrent` can lose cleanup under concurrent writers** (Medium). `ConcurrentCache.java:220-222` evicts the current entry and then sets the new one as two separate steps. Two concurrent callers can both evict (one gets the old entry, the other `null`) and then both set, so the first caller's new entry is overwritten without being scheduled for cleanup. Swap atomically with `getAndSet` and evict the returned value.

3. **Expiry can evict a newly installed current entry** (Medium). `ConcurrentCache.java:304` checks that the current entry has expired and then calls `evictCurrentKey()`, which removes whichever entry is current at that moment. If `putCurrent` runs between the check and the removal, the fresh entry is evicted and closed after the grace period while callers still expect it. Remove only the entry that was checked, with compare-and-set.

4. **`clear()` can drop entries added concurrently without closing them** (Low). `ConcurrentCache.java:403-404` snapshots the map and then clears it in a separate step. An entry inserted in between is removed without being scheduled for cleanup.

5. **Runtime TTL changes are not safely published** (Low). `ConcurrentCache.java:72-74` declare the TTL and grace-period fields as plain mutable fields, with public setters that run on a different thread from the cache and scheduler methods. Without `volatile` (or equivalent) another thread need not observe an update, which the class documents as supported runtime tuning.

6. **Map overlay throws when the types conflict** (Low). `MapUtilsInternal.java:88` casts the existing target value to `Map` whenever the incoming value is a map. Overlaying a map onto a key that holds a scalar or list throws `ClassCastException`, and a present key with a `null` value throws `NullPointerException`. Define the conflict semantics and test both directions.

7. **Overlay recursion has no cycle protection** (Low). `MapUtilsInternal.overlay`/`copyTo` recurse into nested maps and lists without tracking visited objects, so a self-referencing structure ends in `StackOverflowError`.

## Notes

- The cache has no size bound, and a grace period does not guarantee a value stays open for an arbitrarily long operation; callers must account for both.
- `CollectionReconciler`, `MapBuilder`, `ListBuilder` and `MapUtils` were read and have no actionable finding.
