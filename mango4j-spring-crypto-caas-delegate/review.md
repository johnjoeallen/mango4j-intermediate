# Code and security review

Static review of all production Java classes and the module POM, with each finding checked against the code. Findings marked *reproduced* were also demonstrated by running the code. Dependencies were not scanned for known vulnerabilities.

**Quality gates:** mutation 97% (140/145), line coverage 99%; PIT thresholds are set to 96/98.

## Findings

1. **Plain-HTTP endpoints are accepted for cryptographic requests** (High). `RestTemplateBuilder.java:66` registers a plain-HTTP socket factory next to the TLS one, and `CaasDelegateConfig` does not validate the scheme of `caasEndpointBase`. A misconfigured `http://` endpoint sends plaintext or Base64-encoded data and key aliases in the clear. Reject any scheme other than `https`.

2. **An empty trust-store password or location silently widens trust** (High). `SslFactory.java:70` falls back to the JVM's default trust managers when the location is empty or the password is empty. A custom trust store configured with a blank password is ignored and every default CA becomes trusted.

3. **There is no response or socket timeout** (Medium). `RestTemplateBuilder.java:34-39` sets only connect and connection-request timeouts. Apache HttpClient 5's default response timeout is unlimited, so a server that accepts the connection and stalls holds a request thread and a pooled connection indefinitely.

4. **Remote error bodies are logged unfiltered** (Medium). `ProcessCaasService.java:106` logs `ex.getResponseBodyAsString()` at warning level, and `CaasCryptoGateway.java:97,142` log the whole failed batch item (including its payload). If the service or an intermediary echoes request data, it is retained in logs.

5. **Batch HMAC updates can be partial, and a missing item throws** (Medium). `CaasBasicEncryptionServiceDelegate.java:70` updates holders one at a time with `batchResponse.get(requestId).getPayload()`, so a missing or malformed item throws `NullPointerException` after earlier holders have already been changed.

6. **A non-standard status code aborts the batch** (Low). `CaasCryptoGateway.java:91,138` use `HttpStatus.valueOf(int)`, which throws for a code that is not an enum constant.

7. **The connection-validation setting is overwritten** (Low). `RestTemplateBuilder.java:34` replaces the default connection configuration created in `poolingConnectionManager()` (which carried `validateAfterInactivity`) with one containing only the connect timeout.

8. **The HTTP client and its monitor thread have no owned shutdown** (Low). `CaasBasicEncryptionServiceDelegate.java:35` builds the client directly, so the `@PreDestroy` on `CaasRestTemplate` (`:29`) runs only if a Spring container manages that object, which it does not. The pool monitor thread and connections stay open.

## Notes

- Positive: the hostname verifier is `HttpsSupport.getDefaultHostnameVerifier()` and there is no trust-all manager or disabled verification anywhere in this module.
- The store and password settings accept either a value or the name of an environment variable.
