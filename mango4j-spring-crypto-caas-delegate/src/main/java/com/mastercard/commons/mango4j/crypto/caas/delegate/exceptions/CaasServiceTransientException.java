package com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.getRequestId;

/**
 * A transient failure of the CaaS Service (connection failure, service unavailable, internal server error, etc).
 *
 * Extends RestClientException so that the Web ExceptionHandler can treat it as Downstream Service Unavailable error.
 */
public class CaasServiceTransientException extends RestClientException {    //NOSONAR ok to have more than 5 parents

	private static final String SPRING_NESTED_SEPARATOR = "; nested exception is";

	public CaasServiceTransientException(String message, Throwable cause) {
		super(message, cause);
	}

	public static CaasServiceTransientException createWithUrlAndHttpCause(String url, HttpStatusCodeException cause) {
		return new CaasServiceTransientException(
				String.format("CaaS service failure for url=%s: statusCode=%s, requestId=%s, error=%s",
						url, cause.getStatusCode(), getRequestId(cause.getResponseHeaders()), cause.getResponseBodyAsString()),
				cause);
	}

	public static CaasServiceTransientException createWithUrlAndGenericCause(String url, Exception cause) {
		return new CaasServiceTransientException(
				String.format("CaaS service failure for url=%s", url),
				cause);
	}

	@Override
	public String getMessage() {
		// revert the getMessage because NestedRuntimeException.getMessage creates a long a horrible message
		return StringUtils.substringBefore(super.getMessage(), SPRING_NESTED_SEPARATOR);
	}

}
