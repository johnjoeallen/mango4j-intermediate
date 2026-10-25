package com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class CaasServiceTransientExceptionTest {

	private static final String REQUEST_ID_HEADER_NAME = "request-id";
	private static final String TEST_ERROR_MESSAGE = "Test error message";

	@Test
	void constructorWithCause() {
		RuntimeException cause = new RuntimeException("Test cause Exception Message");

		CaasServiceTransientException caasBatchItemException = new CaasServiceTransientException(TEST_ERROR_MESSAGE, cause);

		assertThat(caasBatchItemException.getCause()).isSameAs(cause);
		assertThat(caasBatchItemException.getMessage()).isEqualTo(TEST_ERROR_MESSAGE);
	}

	@Test
	void createWithRequestIdAndBatchItem() {
		String testUrl = "Test URL";
		String testRequestIdHeaderValue = "Test request ID Header value";
		HttpHeaders headers = new HttpHeaders();
		headers.put(REQUEST_ID_HEADER_NAME, List.of(testRequestIdHeaderValue));
		HttpClientErrorException cause = HttpClientErrorException.create(INTERNAL_SERVER_ERROR,
				"Test status text", headers, TEST_ERROR_MESSAGE.getBytes(UTF_8), UTF_8);

		CaasServiceTransientException caasBatchItemException = CaasServiceTransientException.createWithUrlAndHttpCause(testUrl, cause);

		assertThat(caasBatchItemException.getCause()).isSameAs(cause);
		assertThat(caasBatchItemException.getMessage()).isEqualTo(String.format("CaaS service failure for url=%s: statusCode=%s, requestId=%s, error=%s", testUrl, INTERNAL_SERVER_ERROR, testRequestIdHeaderValue, TEST_ERROR_MESSAGE));
	}

	@Test
	void createWithUrlAndGenericCause() {
		String testUrl = "Test URL";
		String testRequestIdHeaderValue = "Test request ID Header value";
		HttpHeaders headers = new HttpHeaders();
		headers.put(REQUEST_ID_HEADER_NAME, List.of(testRequestIdHeaderValue));
		HttpClientErrorException cause = HttpClientErrorException.create(INTERNAL_SERVER_ERROR,
				"Test status text", headers, TEST_ERROR_MESSAGE.getBytes(UTF_8), UTF_8);

		CaasServiceTransientException caasBatchItemException = CaasServiceTransientException.createWithUrlAndGenericCause(testUrl, cause);

		assertThat(caasBatchItemException.getCause()).isSameAs(cause);
		assertThat(caasBatchItemException.getMessage()).isEqualTo(String.format("CaaS service failure for url=%s", testUrl));
	}
}