# Code and security review

Scope: production Java classes, module POM, and selected tests; dependency vulnerabilities were not independently assessed.

## Findings

- **Replacing a keyed entry loses its cleanup** — `src/main/java/ie/bitstep/mango/collections/ConcurrentCache.java:180` discards the prior value returned by `put` without moving it to the eviction set. Replacing a key holding an `AutoCloseable` therefore never closes that value through the cache, including on shutdown. For cached encryption key holders this can leave key material alive until garbage collection. Move the replaced entry to delayed eviction, taking care with concurrent operations and repeated instances, and test replacement and shutdown.
- **Concurrent current-entry writes can lose cleanup** — `ConcurrentCache.java:220` evicts the old entry and then separately sets the new entry. Two concurrent writers can both evict before either sets; the later set overwrites the earlier new entry without scheduling it for cleanup. Atomically swap the entry and evict the returned old value; test concurrent replacement.
- **Runtime TTL changes lack safe publication** — `ConcurrentCache.java:82` declares the durations as ordinary mutable fields, while public setters and cache/scheduler methods may run on different threads. Without a happens-before relationship, other threads need not observe updates, contrary to the documented runtime tuning behavior. Use safe publication (for example volatile fields) and test cross-thread configuration.

The offline Maven test run executed 88 tests, with 39 errors because Mockito could not initialize its mock maker in this environment; the other 49 passed. The cache tests therefore did not provide useful verification.
- **Clear can discard entries added concurrently without closing them** — `ConcurrentCache.java:381` snapshots the map and then clears it in a separate operation. An entry inserted after the snapshot and before `clear()` is removed without being scheduled for cleanup. Coordinate clearing with writes, or remove entries individually with an ownership protocol, and test the interleaving.
- **Expiry can evict a newly installed current entry** — `ConcurrentCache.java:299` checks one current entry for expiry, then `evictCurrentKey()` unconditionally removes whichever entry is current at that later time. A concurrent replacement between the check and removal causes the fresh value to be evicted. Remove only the entry actually checked using compare-and-set; test the interleaving.
- **Map overlays fail on conflicting value types** — `src/main/java/ie/bitstep/mango/collections/MapUtilsInternal.java:79` casts an existing target value to `Map` whenever the incoming value is a map. Merging a map-valued entry over a scalar or list with the same key throws `ClassCastException` instead of overlaying it. Define and implement type-conflict semantics, with tests in both directions.

Additional scope: reviewed the remaining production collection builders and map utilities, the module POM, and selected tests. Dependency vulnerability analysis was not performed. The cache has no capacity bound and a grace period does not guarantee a value remains open for arbitrarily long operations; callers must account for both documented limits.


## Re-assessment (2026-10-31)

- **Altered:** the Mockito initialisation errors recorded above do not reproduce in the current build; all collections tests pass under `-Phammer-time` (mutation 99%, line coverage 98%). The cache concurrency findings are still untested, which is where the remaining 1% of mutants and 2% of coverage sit.
- **Verified in code:** `ConcurrentCache.put` (line ~180) discards the previous value without evicting it, and `putCurrent` evicts then sets as two separate steps. Both findings stand.
- Not re-verified this pass: the clear/expiry/overlay findings are carried forward unchanged.
