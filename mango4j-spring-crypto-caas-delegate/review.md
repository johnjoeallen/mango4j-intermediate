# Code and security review

Reviewed the production classes and configuration. This is a static review; tests were not run and dependency vulnerabilities were not independently assessed.

## Finding

- **Remote error bodies are logged without filtering** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/ProcessCaasService.java:108` writes the complete HTTP error response body to warning logs. If the service or an intermediary includes sensitive request data or credentials in an error response, these can be retained in logs; attacker-controlled response content can also pollute log records. Log a bounded, sanitized error code or explicitly approved fields instead, and test responses containing secrets and control characters.

Tests were not run.
- **Plain HTTP endpoints are accepted for cryptographic requests** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/config/RestTemplateBuilder.java:66` registers a plain HTTP socket, while `CaasDelegateConfig` does not validate the endpoint scheme. A misconfigured `http://` endpoint sends plaintext or Base64-encoded sensitive inputs without TLS. Require HTTPS by default and fail startup for insecure endpoints; test endpoint validation.
- **No response timeout is configured** — `RestTemplateBuilder.java:35` sets connection and connection-pool acquisition timeouts but no response/read timeout. A server that accepts a connection and stalls can occupy request threads and pooled connections indefinitely. Configure a bounded response timeout and test a stalled server.
- **Batch HMAC updates can be partial on malformed responses** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/basic/CaasBasicEncryptionServiceDelegate.java:68` mutates holders one at a time while looking up response items. A missing or malformed item can throw after earlier holders were changed, leaving a mix of cleartext and HMAC values in the input collection. Validate the complete response mapping before mutating any holders and test missing, duplicate, and unexpected IDs.
- **Batch error logging includes the entire response item** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/CaasCryptoGateway.java:96` serializes and logs failed response items, including their payloads; the batch path at line 141 does the same. Remote errors that echo sensitive data can be retained in logs and exceptions. Log sanitized, bounded error metadata and test sensitive response bodies.

Additional scope: reviewed gateway, basic and advanced delegates, client construction, TLS factory, and configuration. Remaining models and transport helpers were reviewed subsequently; tests were not run and dependencies were not independently assessed. No tests were run.
- **Delegate-created HTTP clients and monitor threads lack an owned shutdown path** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/basic/CaasBasicEncryptionServiceDelegate.java:35` creates a `CaasRestTemplate` directly, while its `@PreDestroy` cleanup in `net/CaasRestTemplate.java:29` only runs automatically for a Spring-managed bean. The delegate does not expose or invoke that cleanup. Recreating delegates can leak connection pools and non-daemon monitor threads, and prevent JVM shutdown. Make the delegate closeable or inject a managed client and test lifecycle shutdown.
- **Trust-store misconfiguration silently broadens trust** — `src/main/java/ie/bitstep/mango/spring/crypto/caas/delegate/ssl/SslFactory.java:70` falls back to default trust managers when either the trust-store location or password is empty. A configured custom trust store with an accidentally empty password is ignored, potentially accepting certificates trusted by the system rather than the intended store. Fail closed on partially specified custom configuration, while explicitly supporting a valid empty-password store if intended; test both cases.
- **Connection validation settings are overwritten** — `config/RestTemplateBuilder.java:34` replaces the default connection configuration created in `poolingConnectionManager()` with one containing only the connect timeout. The configured validate-after-inactivity setting is therefore lost. Build one combined configuration and test that both values reach the connection manager.

Reviewed the remaining models, exceptions, transport helpers, TLS key manager, and module configuration. No tests were run; third-party dependency vulnerabilities were not independently assessed.


## Re-assessment (2026-10-31)

- **Verified in code:** `RestTemplateBuilder` registers `PlainConnectionSocketFactory` for `http` alongside the TLS factory (line ~66), and `SslFactory.getTrustManagers` falls back to the JVM default trust managers when the trust-store location is empty **or** the password is empty. Both findings stand; the second means a configured custom trust store with a blank password is silently ignored and trust widens to every default CA.
- Positive: the hostname verifier is `HttpsSupport.getDefaultHostnameVerifier()` and there is no trust-all manager, no disabled verification and no `X509TrustManager` override anywhere in production code.
- **Altered:** mutation 97% (140/145) and coverage 99%; thresholds lowered from 100 to 96/98. Five surviving mutants in a module that handles key material and TLS.
