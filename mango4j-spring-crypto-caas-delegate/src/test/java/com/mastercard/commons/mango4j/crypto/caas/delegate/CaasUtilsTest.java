package com.mastercard.commons.mango4j.crypto.caas.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Constructor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CaasUtilsTest {

	private static final String TEST_REQUEST_ID = "TEST_REQUEST_ID";
	private static final String TEST_RESPONSE_TIME = "16ms";
	private static final String TEST_VALUE = "тест value";
	private static final String TEST_VALUE_B64 = "0YLQtdGB0YIgdmFsdWU=";
	private static final String REQUEST_ID_HEADER_NAME = "request-id";
	private static final String RESPONSE_TIME_HEADER_NAME = "response-time";

	@Mock
	private ObjectMapper mockObjectMapper;

	private final HttpHeaders httpHeaders = new HttpHeaders();
	private final TestDTO testDTO = new TestDTO();

	@Test
	void privateConstructor() throws Exception {
		Constructor<CaasUtils> constructor = CaasUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThat(constructor.newInstance()).isInstanceOf(CaasUtils.class);
	}

	@Test
	void getRequestIdWhenNotPresent() {
		String requestId = CaasUtils.getRequestId(httpHeaders);

		assertThat(requestId).isEmpty();
	}

	@Test
	void getRequestIdWhenPresent() {
		httpHeaders.put(REQUEST_ID_HEADER_NAME, singletonList(TEST_REQUEST_ID));

		String requestId = CaasUtils.getRequestId(httpHeaders);

		assertThat(requestId).isEqualTo(TEST_REQUEST_ID);
	}

	@Test
	void getRequestIdWhenNullHeaders() {
		String requestId = CaasUtils.getRequestId(null);

		assertThat(requestId).isEmpty();
	}

	@Test
	void getResponseTimeWhenNotPresent() {
		String responseTime = CaasUtils.getResponseTime(httpHeaders);

		assertThat(responseTime).isEmpty();
	}

	@Test
	void getResponseTimeWhenPresent() {
		httpHeaders.put(RESPONSE_TIME_HEADER_NAME, singletonList(TEST_RESPONSE_TIME));

		String responseTime = CaasUtils.getResponseTime(httpHeaders);

		assertThat(responseTime).isEqualTo(TEST_RESPONSE_TIME);
	}

	@Test
	void getResponseTime_WhenNullHeaders() {
		String responseTime = CaasUtils.getResponseTime(null);

		assertThat(responseTime).isEmpty();
	}

	@Test
	void toJsonString() throws JsonProcessingException {
		given(mockObjectMapper.writeValueAsString(testDTO)).willReturn(TEST_VALUE);

		String actual = CaasUtils.toJsonString(mockObjectMapper, testDTO);

		assertThat(actual).isEqualTo(TEST_VALUE);
	}

	@Test
	void toJsonStringObjectMapperException() throws JsonProcessingException {
		RuntimeException cause = new RuntimeException();
		given(mockObjectMapper.writeValueAsString(testDTO)).willThrow(cause);

		assertThatThrownBy(() -> CaasUtils.toJsonString(mockObjectMapper, testDTO))
				.isInstanceOf(IllegalStateException.class)
				.hasCause(cause)
				.hasMessage("java.lang.RuntimeException");
	}

	@Test
	void base64Encode() {
		String result = CaasUtils.base64Encode(TEST_VALUE);

		assertThat(result).isEqualTo(TEST_VALUE_B64);
	}

	@Test
	void base64EncodeWhenEmpty() {
		String result = CaasUtils.base64Encode("");

		assertThat(result).isEmpty();
	}

	@Test
	void base64EncodeWhenNull() {
		String result = CaasUtils.base64Encode(null);

		assertThat(result).isNull();
	}

	@Test
	void base64Decode() {
		String result = CaasUtils.base64Decode(TEST_VALUE_B64);

		assertThat(result).isEqualTo(TEST_VALUE);
	}

	@Test
	void base64DecodeWhenEmpty() {
		String result = CaasUtils.base64Decode("");

		assertThat(result).isEmpty();
	}

	@Test
	void base64DecodeWhenNull() {
		String result = CaasUtils.base64Decode(null);

		assertThat(result).isNull();
	}

	private static class TestDTO {
		public String value = TEST_VALUE;
	}
}