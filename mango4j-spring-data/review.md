# Code and security review

Reviewed the module's production Java source, `OffsetLimitPageable`. Tests and dependency vulnerabilities were not independently assessed.

## Finding

- **Page number silently overflows for large offsets** — `src/main/java/ie/bitstep/mango/spring/jpa/OffsetLimitPageable.java:35` casts a long quotient to int without checking its range. For example, offset 2147483648 and limit 1 produce a negative page number despite passing constructor validation. Consumers using `getPageNumber()` can navigate to or report the wrong page. Reject offsets whose quotient exceeds `Integer.MAX_VALUE`, or use a checked conversion, and test the boundary. `next()` can also overflow a long near `Long.MAX_VALUE` and then throws through constructor validation; document or explicitly check that boundary.

No tests were run.


## Re-assessment (2026-10-31)

- No new findings. The module is one class (`OffsetLimitPageable`, 77 lines) and the overflow finding stands as written.
- **Altered:** line coverage is 95% (19/20 lines) and its PIT coverage threshold was lowered from 100 to 95; one line is untested. The module declares `spring-boot-starter-data-jpa` at compile scope for this single class, which forces JPA and Hibernate onto every consumer.
