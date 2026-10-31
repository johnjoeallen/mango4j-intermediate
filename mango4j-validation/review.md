# Code and security review

Reviewed the nine production Java classes and constraint definitions. Test execution and third-party dependency vulnerability analysis were not performed.

## Finding

- **Empty identifiers pass both naming validators** — `src/main/java/ie/bitstep/mango/validation/validators/IdentifierValidator.java:47` checks character membership and separator placement but does not require a character. The empty string passes these checks and hence both `@IsValidKebabCase` and `@IsValidDottedCase`. Where these constraints alone protect a required identifier, empty values are accepted. Require a nonempty string if the intended contract requires a name, or document that callers must combine these constraints with `@NotBlank`; add tests for empty and null inputs.

The UUID constraints intentionally permit null under Bean Validation's `@Pattern` semantics; applications needing a required value must also apply `@NotNull`.


## Re-assessment (2026-10-31)

- No new findings. The empty-identifier finding was carried forward without re-verification.
- **Altered:** line coverage is 86% (24/28). The PIT coverage threshold was lowered from 100 to 85, so four lines have no test at all; check whether they include the empty-input path above.
