package com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions;

import org.springframework.web.client.HttpClientErrorException;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.getRequestId;

/**
 * Failure of the CaaS crypto operation (invalid key alias, invalid data to decrypt, etc).
 */
public class CaasCryptoException extends RuntimeException {

	public CaasCryptoException(String message) {
		super(message);
	}

	public CaasCryptoException(String message, Throwable cause) {
		super(message, cause);
	}

	public static CaasCryptoException createWithUrlAndHttpCause(String url, HttpClientErrorException cause) {
		return new CaasCryptoException(
				String.format("Failed to perform CaaS operation for url=%s: statusCode=%s, requestId=%s, error=%s",
						url, cause.getStatusCode(), getRequestId(cause.getResponseHeaders()), cause.getResponseBodyAsString()),
				cause);
	}

}
