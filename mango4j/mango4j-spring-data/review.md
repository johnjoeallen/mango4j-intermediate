# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 100%, line coverage 95% (19/20); the coverage threshold is set to 95.

## Findings

1. **The page number overflows for large offsets** (Low). `OffsetLimitPageable.java:35` casts a `long` quotient to `int`. An offset of 2147483648 with limit 1 passes constructor validation and gives a negative page number.

2. **There is no upper bound on the page size** (Low). `OffsetLimitPageable.of` accepts any limit up to `Integer.MAX_VALUE`; if it is built from request parameters, the caller must cap it.

3. **JPA is a compile-scope dependency of a one-class module** (Low). `pom.xml:53` declares `spring-boot-starter-data-jpa` at compile scope for one class that needs only Spring Data's `Pageable`, forcing JPA and Hibernate onto every consumer.
