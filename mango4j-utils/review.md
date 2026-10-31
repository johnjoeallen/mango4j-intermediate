# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 98% (279/284), line coverage 98%; PIT thresholds are set to those values.

## Findings

1. **`ObjectMutator` skips objects that are equal to ones it already visited, and keeps state between calls** (High). `ObjectMutator.java:73` pushes the object onto the `visited` stack in `mutate(Object)` and never pops it, and `mutateValue` (`:136-143`) pops once for the two pushes it causes, so every nested object leaves an entry behind. Cycle detection then uses `contains`, which compares with `equals`. *Reproduced:* with a child type whose `equals` compares only an id, two sibling children with the same id are visited once (`a=[hi] b=hi c=[hi]`), and a second `mutate` call on the same mutator instance changes nothing at all (`a=hi b=hi c=hi`). When the mutators sanitise input (the bundled `HtmlEscapeMutator`, `@Text`), values are silently left unsanitised. Pop what `mutate` pushes, compare by identity, and reset state per top-level call.

2. **Collection mutation empties the collection on failure** (Medium). `ObjectMutator.java:207` clears the original collection before transforming and re-adding its elements. If a mutator or nested traversal throws, the collection is left empty or partly rebuilt. Build the replacement first and swap it in only on success.

3. **`getEndOfMonth(int, TimeZone)` returns the wrong month near month end** (Medium). `DateUtils.java:137-142` sets only the month on a calendar that still has today's day-of-month, then `getEndOfMonth(Calendar)` calls `getActualMaximum`, which forces the lenient rollover. *Reproduced* on the 31st: `getEndOfMonth(10, UTC)` (November) returned `2026-12-01T23:59:59.999Z` instead of `2026-11-30T23:59:59.999Z`. (`getBeginningOfMonth(int, TimeZone)` is correct because the calendar is not resolved before the day is set.) Set the day to 1 before changing the month.

4. **URL path segments are form-encoded, which changes their meaning** (Medium). `URLGenerator.java:128` encodes path segments with `URLEncoder`, so a space becomes `+`, which in a path is a literal plus. `path()` (`:234`) decodes with `URLDecoder`, so `%20` in an input becomes a space and is re-emitted as `+`. *Reproduced:* `ofURL("https://h/a%20b")` is rendered as `https://h/a+b`. A request can then address a different resource than intended. Use RFC 3986 path encoding.

5. **Encoded query delimiters are treated as delimiters** (Medium). `URLGenerator.initialiseQuery` (`:247`) takes the already-decoded `URI.getQuery()` and splits it on `&`. *Reproduced:* `?q=a%26b%3Dc` becomes `q=a&b=c`, two parameters instead of one, and escaped characters can be decoded twice. Split `getRawQuery()` first and decode each part.

6. **`param()` appends instead of replacing** (Low). The Javadoc for `URLGenerator.param` (`:206`) says it replaces an existing parameter with the same key, but it appends another value. *Reproduced:* `param("k","1").param("k","2")` renders `?k=1&k=2`.

7. **`QueryParam.create` throws on valid input and drops data** (Low). `QueryParam.java:25` splits on every `=` and reads `parts[1]`. *Reproduced:* `a=` and `a` throw `ArrayIndexOutOfBoundsException`, and `a=b=c` yields the value `b`. Split on the first `=` only and define the no-value behaviour.

8. **The UUIDv7 generator never fills `rand_a`** (Medium). `UUIDv7.java:43` sets the version bits and leaves the 12-bit `rand_a` field at zero, so only the 62 bits in the low half vary within a millisecond. *Reproduced:* 200 generated ids all have `rand_a == 0`. It also provides no ordering inside a millisecond. (`mango-swarm` has its own correct `UuidV7`; consider sharing one.)

9. **`MovingClock` breaks the equals/hashCode contract** (Low). `MovingClock.java:47` compares the reference clock and `offsetMs`, but `hashCode` (`:57`) hashes `incrementMillis` twice and omits the offset. *Reproduced:* two clocks that are equal have different hash codes, so hash-based collections misbehave.

10. **Tolerance and reduction bounds overflow at the extremes** (Low). `TolerateMutator.java:48` compares against `max + 1` and `min - 1` in `long`, and `ReduceMutator.java:33` does the same in `int`. With a bound of `Long.MAX_VALUE` / `Integer.MAX_VALUE` the addition wraps, so every value is clamped (or `substring` fails). Compare with `>` and `<` directly.

11. **`MapFormat` mishandles map values and mixed paths** (Low). `MapFormat.java:325,370` descend whenever a value is a map without checking whether the path has another segment. *Reproduced:* formatting `{item}` where `item` is a map throws `ArrayIndexOutOfBoundsException`, and `{a.b}` where `a` is a string is treated as a match and renders `scalar`.

12. **`EntityToStringBuilder` prints every string field** (Low). `EntityToStringBuilder.java:61` accepts all primitive, string, number and date fields by reflection, with no way to exclude a sensitive field, so a password or token field on an entity appears in `toString()` output.

## Notes

- `MappingUtils` uses Jackson 3 without default typing, so it does not enable polymorphic deserialization of untrusted JSON.
- `NamedScheduledExecutorBuilder` and the remaining small helpers were read and have no actionable finding.
