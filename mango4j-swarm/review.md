# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities. PostgreSQL concurrency behaviour was assessed from the SQL, not by running a database.

**Quality gates:** mutation 93% (643/691), line coverage 97% (1343/1387); PIT thresholds are set to 93/97. A PIT run takes about 10 minutes.

## Findings

1. **Timeout recovery can overwrite a task that completed or was reclaimed concurrently** (Medium). `JdbcTaskRepository.java:129` selects timed-out task ids in a subquery, but the outer `UPDATE ... WHERE id IN (...)` does not re-check the status. If a task completes or is claimed again between the subquery and the update, the update sets it back to `queued`. Re-check `status IN ('claimed','in_progress')` and the timeout predicate in the outer `WHERE`.

2. **A worker runs a task even if it lost its claim before starting** (Medium). `MangoSwarmDaemon.java:443` calls `markInProgress` and then `executeTask` unconditionally; the guarded `UPDATE` affects zero rows if the claim was lost, and the return is ignored. Timeout recovery can then run the task on another worker as well.

3. **Stale progress reports overwrite the active attempt** (Medium). `JdbcTaskRepository.java:72` upserts the runtime row by task id and replaces `worker_id` and the progress fields without checking the owning worker or attempt, so a timed-out worker still reporting progress corrupts the new attempt's runtime state.

4. **Recurring follow-up tasks are not fenced or idempotent** (Medium). `MangoSwarmDaemon.requestAgain` (`:527`) inserts the new task immediately, independent of the current attempt's outcome. A crash, retry or timeout after the insert runs the handler again and inserts another follow-up.

5. **The Swarm schema is applied to Hibernate's default schema by default** (Medium). `MangoSwarmProperties.java:372` defaults `applySchemaToHibernateDefault` to `true`, and `MangoSwarmAutoConfiguration.java:109` then sets `hibernate.default_schema` when the application has not. That changes where every entity in the application resolves, not only Swarm's tables.

6. **Explicit handler failure details are discarded** (Low). `MangoSwarmDaemon.java:478` detects `TaskExecutionResult.Failed failed` but passes a constant message to `handleFailedTask` instead of `failed.message()`.

7. **`Error`s from a handler are not handled** (Low). `MangoSwarmDaemon.java:507` catches `Exception`, so an `Error` (for example `OutOfMemoryError` or `NoClassDefFoundError`) leaves the task `in_progress` until it times out.

8. **Schema validation and UUID generation are duplicated** (Low). The same identifier regex and length check live in both `MangoSwarmProperties.java:18` and `SchemaQualifiedTables.java:10`, and `UuidV7.java` duplicates `mango4j-utils`' generator (this copy fills `rand_a` correctly).

## Notes

- There is no SQL injection: all statements are static and parameterised, and the only dynamic value, the schema name, is validated against `[A-Za-z_]\w*` (maximum 63 characters) and applied through `set_config('search_path', ?, true)`, scoped to the transaction.
- Timeout reclamation can cause overlapping execution if the old handler keeps running after its lease expires; handlers must be idempotent and use fencing where the downstream system supports it.
- The package is `ie.bitstep.mango.spring.swarm`.
