# Code and security review

Reviewed both production classes, the module POM, and the delegate tests. This is a static review; no tests were run, no live AWS calls were made, and dependency vulnerabilities were not independently assessed.

## Findings

- **Decryption does not bind the ciphertext to the configured KMS key** — `src/main/java/ie/bitstep/mango/crypto/delegates/aws/kms/impl/service/encryption/AwsKmsEncryptionServiceDelegate.java:63` sends only the ciphertext blob to KMS, ignoring the `CryptoKey` associated with the container and the key ID saved during encryption. If the application's KMS principal can decrypt under multiple keys, a substituted ciphertext can be decrypted under a different key than the caller's selected key, undermining key or tenant isolation assumptions. Set the expected KMS key from trusted key configuration and verify the returned key identity, accounting for aliases and rotation; test mismatched keys and tenant contexts.
- **Asymmetric encryption configuration is not propagated to decryption** — the encrypt method sends the configured algorithm and persists the response algorithm, but the decrypt request at line 63 omits both the algorithm and key ID. KMS asymmetric decryption requires these parameters, so ciphertext produced with an asymmetric key cannot round-trip through this delegate. Populate the request using trusted key configuration and validated stored metadata; add a KMS-compatible asymmetric round-trip test.

The existing mocked decrypt test explicitly expects both fields to be null, so it does not exercise either constraint. KMS encryption context is not supported by this interface; applications needing context-bound ciphertext must account for that limitation.


## Re-assessment (2026-10-31)

- No new findings. Both existing findings were carried forward without re-verification.
- Unchanged from the previous pass: no live AWS call was made, and the delegate's decrypt test asserts null key ID and algorithm, so neither constraint is exercised.
