# Code and security review

Reviewed production Java classes and the module POM. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Findings

- **Path encoding changes URL meaning** — `src/main/java/ie/bitstep/mango/utils/url/URLGenerator.java:126` uses form encoding for path segments, turning spaces into `+`. In a URL path, `+` is a literal plus rather than a space. Likewise, `path()` at line 219 decodes literal plus signs as spaces. A path containing a space or plus can therefore address the wrong resource. Use percent encoding appropriate to URI path segments and add round-trip tests for spaces and plus signs.
- **Encoded query delimiters are parsed as delimiters** — `initialiseQuery()` at line 236 obtains the decoded query with `URI.getQuery()` before splitting it on `&`. A value containing `%26` is split into separate parameters, and other escaped characters can be decoded twice. This can alter request semantics and potentially bypass assumptions made by callers about parameter boundaries. Split the raw query before decoding each key and value once, with tests for `%26`, `%3D`, `%25`, and `+`.
- **Parameter API contradicts its contract** — `param()` at line 193 documents replacement, but appends another value. Callers relying on replacement may send conflicting parameters, whose interpretation varies by server. Either replace existing values or explicitly expose and document append semantics.

No tests were run for this partial review.

Additional findings:

- **UUIDv7 generator leaves 12 random bits fixed** — `src/main/java/ie/bitstep/mango/utils/uuid/UUIDv7.java:43` sets the version but never fills the 12-bit `rand_a` field. Only the 62 random bits in the least-significant half vary within a millisecond, increasing collision risk versus a full random UUIDv7. Fill both random fields and add a test checking variation in `rand_a`; UUIDs should not be treated as authorization secrets.
- **Mutator retains visited objects and can mishandle cyclic graphs** — `src/main/java/ie/bitstep/mango/utils/mutator/ObjectMutator.java:70` pushes each object but never pops it at the end of `mutate`. The instance retains references across calls and `visited.contains` can suppress subsequent mutations of equal values. Recursive calls add additional stack entries, and exceptions leave the state unbalanced. Manage traversal state per invocation with identity-based cycle detection and `finally` cleanup; test reuse, equal-but-distinct objects and cycles.
- **Collection mutation can destroy data on failure** — `ObjectMutator.java:207` clears the original collection before transforming elements. If a mutator, nested traversal, or add fails, the collection remains empty or partially rebuilt. Compute the replacement first and apply it only after successful processing; define behavior for unmodifiable collections and test failures.
- **Clock equality violates the hash contract** — `src/main/java/ie/bitstep/mango/utils/clock/MovingClock.java:43` compares reference clock and offset but not increment; `hashCode()` at line 57 includes increment and omits offset. Two equal clocks can have different hashes. Make equality and hashing use the same fields and test hash-based collections.
- **Tolerance bounds overflow at long extremes** — `src/main/java/ie/bitstep/mango/utils/conformance/TolerateMutator.java:45` compares against `max + 1` and `min - 1`. With `Long.MAX_VALUE` or `Long.MIN_VALUE` these wrap, incorrectly clamping valid values. Compare directly with `>` and `<`, validate bounds, and test extreme values.

Additional scope: reviewed the POM and mutator, UUID, clock, conformance, mapping, executor, and entity helpers. The remaining date, formatting, and URL helper classes were reviewed separately. No tests were run.
- **Query parameters with empty or additional equals signs are mishandled** — `src/main/java/ie/bitstep/mango/utils/url/QueryParam.java:25` splits on every `=` and assumes two nonempty parts. `a=` and `a` throw, while `a=b=c` loses `=c`. Split at the first equals sign and explicitly define missing-value behavior; test empty and embedded delimiters.
- **Changing calendar month can roll into a different month** — `src/main/java/ie/bitstep/mango/utils/date/DateUtils.java:84` sets the month on a calendar retaining today's day of month. On the 31st, selecting a shorter month can normalize into the following month before the beginning or end is calculated; `getEndOfMonth(int, TimeZone)` has the same issue. Set the day to 1 before changing the month and test month-end dates using a controllable clock.
- **Map-valued format arguments can throw** — `src/main/java/ie/bitstep/mango/utils/string/MapFormat.java:355` descends whenever a value is a map without checking whether the requested path has another component. Formatting `{item}` when `item` is a map indexes beyond the path array. Conversely, a scalar encountered before the end of a dotted path is treated as a match. Stop traversal at the requested path length and reject a scalar before the final segment; add both tests.


## Re-assessment (2026-10-31)

- **Verified in code:** `UUIDv7.generate()` sets only the version nibble (`msb |= 0x7000L`) and never fills the 12-bit `rand_a` field; `URLGenerator` encodes path segments with `URLEncoder` (form encoding, `+` for space) at line ~126; and `initialiseQuery` splits the already-decoded `URI.getQuery()` on `&`. All three findings stand.
- **New (low): UUIDv7 is not monotonic within a millisecond.** Two UUIDs generated in the same millisecond order by the random bits, so ids are not strictly time-sortable as UUIDv7 implementations usually promise. Add a counter in `rand_a` (which also fixes the unfilled field).
- **Altered:** mutation score is 98% and coverage 98%; its PIT thresholds were lowered from 100 to 98.
