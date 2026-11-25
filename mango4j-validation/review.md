# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 100%, line coverage 86% (24/28); the coverage threshold is set to 85, so four lines have no test.

## Findings

1. **Empty identifiers pass both naming validators** (Medium). `IdentifierValidator.java:47-51` requires only that every character is valid and that the separator is not first, last or doubled. The empty string satisfies all of that, so `@IsValidKebabCase` and `@IsValidDottedCase` accept it. Where the constraint alone guards a required identifier, an empty value gets through; require a minimum length or document that `@NotEmpty` is also needed. (`null` passing is standard Bean Validation behaviour.)

2. **`ValidationUtils.validate` builds a new validator factory on every call** (Low). `ValidationUtils.java:26` calls `Validation.buildDefaultValidatorFactory()` inside the method, which bootstraps the provider each time. Called on a hot path it adds latency and garbage; cache the factory.

3. **`ValidationUtilsException` has no message** (Low). `ValidationUtilsException.java:17` stores the violations but never calls `super(message)`, so `getMessage()` is `null` and logs show an unexplained exception. Build a message from the violations' paths (not their invalid values).

## Notes

- The UUID constraints are lowercase-only patterns; `Type4UUID` does not check the version nibble (its message says "type 4 like"), while `StrictType4UUID` checks both version and variant.
